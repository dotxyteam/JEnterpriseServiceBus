import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrateur transactionnel par étapes avec compensation (rollback). -
 * Chaque étape déclare un nom, une exécution et une compensation. - Statuts
 * riches (NOT_STARTED, IN_PROGRESS, DONE, FAILED, ROLLED_BACK, SKIPPED). -
 * Politique de retry configurable par étape (maxRetries + backoff + filtre
 * d'exceptions retryables). - Rollback garanti en ordre inverse sur les étapes
 * exécutées avec succès. - Journalisation via java.util.logging.
 *
 * Exemple d’utilisation : voir main().
 */
public class TransactionalOrchestrator {

	/* ======================= API ======================= */

	public interface Step {
		String name();

		void execute(StepContext ctx) throws Exception;

		/**
		 * Doit être idempotent autant que possible. Ne lance pas d’exception bloquante.
		 */
		default void rollback(StepContext ctx) {
			// No-op par défaut
		}

		/** Politique de retry spécifique à l’étape (optionnel). */
		default RetryPolicy retryPolicy() {
			return RetryPolicy.none();
		}

		/** Peut être utilisé pour court-circuiter l’étape (préconditions). */
		default boolean shouldRun(StepContext ctx) {
			return true;
		}
	}

	public static final class StepContext {
		private final Map<String, Object> bag = new HashMap<>();

		public void put(String key, Object value) {
			bag.put(key, value);
		}

		@SuppressWarnings("unchecked")
		public <T> T get(String key) {
			return (T) bag.get(key);
		}

		public boolean contains(String key) {
			return bag.containsKey(key);
		}

		public Map<String, Object> asMap() {
			return Collections.unmodifiableMap(bag);
		}
	}

	public enum StepStatus {
		NOT_STARTED, IN_PROGRESS, DONE, FAILED, ROLLED_BACK, SKIPPED
	}

	public static final class StepReport {
		public final String name;
		public final StepStatus status;
		public final int attempts;
		public final Instant startedAt;
		public final Instant endedAt;
		public final Exception error;

		StepReport(String name, StepStatus status, int attempts, Instant startedAt, Instant endedAt, Exception error) {
			this.name = name;
			this.status = status;
			this.attempts = attempts;
			this.startedAt = startedAt;
			this.endedAt = endedAt;
			this.error = error;
		}

		public Duration duration() {
			if (startedAt == null || endedAt == null)
				return Duration.ZERO;
			return Duration.between(startedAt, endedAt);
		}

		@Override
		public String toString() {
			return "StepReport{" + "name='" + name + '\'' + ", status=" + status + ", attempts=" + attempts
					+ ", duration=" + duration().toMillis() + "ms"
					+ (error != null ? ", error=" + error.getClass().getSimpleName() + ": " + error.getMessage() : "")
					+ '}';
		}
	}

	public static final class RunReport {
		public final List<StepReport> steps;
		public final boolean success;
		public final Instant startedAt;
		public final Instant endedAt;

		RunReport(List<StepReport> steps, boolean success, Instant startedAt, Instant endedAt) {
			this.steps = steps;
			this.success = success;
			this.startedAt = startedAt;
			this.endedAt = endedAt;
		}

		public Duration duration() {
			return Duration.between(startedAt, endedAt);
		}

		@Override
		public String toString() {
			return "RunReport{success=" + success + ", steps=" + steps.size() + ", duration=" + duration().toMillis()
					+ "ms}";
		}
	}

	/* ======================= Retry ======================= */

	public static final class RetryPolicy {
		final int maxRetries;
		final Backoff backoff;
		final Predicate<Throwable> retryOn;

		public interface Backoff {
			long nextDelayMs(int attemptIndex); // attemptIndex: 1..maxRetries
		}

		public static RetryPolicy none() {
			return new RetryPolicy(0, attempt -> 0, t -> false);
		}

		public static RetryPolicy fixed(int maxRetries, Duration delay) {
			return new RetryPolicy(maxRetries, attempt -> delay.toMillis(), t -> true);
		}

		public static RetryPolicy exponential(int maxRetries, Duration initialDelay, double factor, Duration maxDelay) {
			return new RetryPolicy(maxRetries, attempt -> {
				double d = initialDelay.toMillis() * Math.pow(factor, attempt - 1);
				return (long) Math.min(d, maxDelay.toMillis());
			}, t -> true);
		}

		public RetryPolicy retryOn(Predicate<Throwable> predicate) {
			return new RetryPolicy(this.maxRetries, this.backoff, predicate);
		}

		private RetryPolicy(int maxRetries, Backoff backoff, Predicate<Throwable> retryOn) {
			this.maxRetries = Math.max(0, maxRetries);
			this.backoff = Objects.requireNonNull(backoff);
			this.retryOn = Objects.requireNonNull(retryOn);
		}
	}

	/* ======================= Orchestrateur ======================= */

	private static final Logger LOG = Logger.getLogger(TransactionalOrchestrator.class.getName());

	public RunReport run(List<Step> steps, StepContext ctx) {
		Objects.requireNonNull(steps, "steps");
		Objects.requireNonNull(ctx, "ctx");

		Instant startedAt = Instant.now();
		List<StepReport> reports = new ArrayList<>(steps.size());
		Deque<StepWithMeta> executed = new ArrayDeque<>();
		@SuppressWarnings("unused")
		boolean ok = false;

		try {
			for (Step s : steps) {
				StepWithMeta meta = new StepWithMeta(s);
				if (!s.shouldRun(ctx)) {
					meta.status = StepStatus.SKIPPED;
					meta.startedAt = Instant.now();
					meta.endedAt = meta.startedAt;
					reports.add(meta.toReport());
					LOG.fine(() -> "SKIPPED step: " + s.name());
					continue;
				}

				LOG.info(() -> "START step: " + s.name());
				meta.status = StepStatus.IN_PROGRESS;
				meta.startedAt = Instant.now();

				Exception lastError = null;
				int attempts = 0;

				RetryPolicy rp = s.retryPolicy();
				int maxAttempts = 1 + Math.max(0, rp.maxRetries);

				while (attempts < maxAttempts) {
					attempts++;
					try {
						s.execute(ctx);
						meta.status = StepStatus.DONE;
						meta.attempts = attempts;
						meta.endedAt = Instant.now();
						reports.add(meta.toReport());
						executed.push(meta); // Pour un rollback éventuel
						LOG.info(() -> "DONE step: " + s.name() + " (attempt " + meta.attempts + ")");
						break;
					} catch (Exception ex) {
						lastError = ex;
						meta.attempts = attempts;
						LOG.log(Level.WARNING,
								"FAIL step: " + s.name() + " (attempt " + attempts + "/" + maxAttempts + ")", ex);

						boolean canRetry = attempts < maxAttempts && rp.retryOn.test(ex);
						if (canRetry) {
							long delayMs = rp.backoff.nextDelayMs(attempts);
							sleepQuietly(delayMs);
							continue;
						} else {
							meta.status = StepStatus.FAILED;
							meta.error = lastError;
							meta.endedAt = Instant.now();
							reports.add(meta.toReport());
							throw ex; // Provoque le rollback global
						}
					}
				}
			}
			ok = true;
			return new RunReport(reports, true, startedAt, Instant.now());

		} catch (Exception fatal) {
			LOG.warning(() -> "Pipeline FAILED. Starting rollback...");
			rollbackAll(executed, ctx, reports);
			return new RunReport(reports, false, startedAt, Instant.now());
		}
	}

	private void rollbackAll(Deque<StepWithMeta> executed, StepContext ctx, List<StepReport> reports) {
		while (!executed.isEmpty()) {
			StepWithMeta meta = executed.pop();
			if (meta.status != StepStatus.DONE)
				continue; // par sécurité
			try {
				LOG.info(() -> "ROLLBACK step: " + meta.step.name());
				meta.step.rollback(ctx);
				// Ajouter un rapport ROLLED_BACK pour traçabilité
				StepWithMeta rb = new StepWithMeta(meta.step);
				rb.status = StepStatus.ROLLED_BACK;
				rb.startedAt = Instant.now();
				rb.endedAt = rb.startedAt;
				rb.attempts = 0;
				reports.add(rb.toReport());
			} catch (Exception ex) {
				// On journalise mais on n’échoue pas le rollback global pour une étape
				LOG.log(Level.SEVERE, "Rollback FAILED for step: " + meta.step.name(), ex);
				StepWithMeta rb = new StepWithMeta(meta.step);
				rb.status = StepStatus.ROLLED_BACK; // on considère "tenté"
				rb.startedAt = Instant.now();
				rb.endedAt = rb.startedAt;
				rb.error = ex;
				reports.add(rb.toReport());
			}
		}
	}

	private static void sleepQuietly(long delayMs) {
		if (delayMs <= 0)
			return;
		try {
			TimeUnit.MILLISECONDS.sleep(delayMs);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	private static final class StepWithMeta {
		final Step step;
		StepStatus status = StepStatus.NOT_STARTED;
		int attempts = 0;
		Instant startedAt;
		Instant endedAt;
		Exception error;

		StepWithMeta(Step step) {
			this.step = step;
		}

		StepReport toReport() {
			return new StepReport(step.name(), status, attempts, startedAt, endedAt, error);
		}
	}

	/* ======================= Exemple d’utilisation ======================= */

	public static void main(String[] args) {
		Logger root = Logger.getLogger("");
		root.setLevel(Level.INFO);

		Step startDb = new Step() {
			@Override
			public String name() {
				return "StartDB";
			}

			@Override
			public void execute(StepContext ctx) throws Exception {
				// Simule une ressource temporairement indisponible au premier essai
				Integer tries = ctx.get("dbTries");
				if (tries == null)
					tries = 0;
				tries++;
				ctx.put("dbTries", tries);
				if (tries < 2) {
					throw new RuntimeException("DB not ready yet");
				}
				ctx.put("dbStarted", true);
				System.out.println("DB started");
			}

			@Override
			public void rollback(StepContext ctx) {
				if (Boolean.TRUE.equals(ctx.get("dbStarted"))) {
					System.out.println("DB stopped");
					ctx.put("dbStarted", false);
				}
			}

			@Override
			public RetryPolicy retryPolicy() {
				return RetryPolicy.exponential(3, Duration.ofMillis(200), 2.0, Duration.ofSeconds(2))
						.retryOn(ex -> ex instanceof RuntimeException);
			}
		};

		Step initCache = new Step() {
			@Override
			public String name() {
				return "InitCache";
			}

			@Override
			public void execute(StepContext ctx) {
				ctx.put("cache", new HashMap<String, String>());
				System.out.println("Cache initialized");
			}

			@Override
			public void rollback(StepContext ctx) {
				Map<?, ?> cache = ctx.get("cache");
				if (cache != null) {
					cache.clear();
					System.out.println("Cache cleared");
				}
			}
		};

		Step startWeb = new Step() {
			@Override
			public String name() {
				return "StartWebServer";
			}

			@Override
			public void execute(StepContext ctx) throws Exception {
				// Simule une panne fatale
				throw new IllegalStateException("Port already in use");
			}

			@Override
			public void rollback(StepContext ctx) {
				System.out.println("Web server stopped (noop in example)");
			}

			@Override
			public RetryPolicy retryPolicy() {
				// On choisit de ne PAS retenter les IllegalStateException
				return RetryPolicy.fixed(2, Duration.ofMillis(300))
						.retryOn(ex -> !(ex instanceof IllegalStateException));
			}
		};

		TransactionalOrchestrator orch = new TransactionalOrchestrator();
		StepContext ctx = new StepContext();
		List<Step> steps = Arrays.asList(startDb, initCache, startWeb);

		RunReport report = orch.run(steps, ctx);

		System.out.println("=== RUN REPORT ===");
		System.out.println("Success: " + report.success + " | Duration: " + report.duration().toMillis() + "ms");
		for (StepReport r : report.steps) {
			System.out.println(" - " + r);
		}
	}
}
