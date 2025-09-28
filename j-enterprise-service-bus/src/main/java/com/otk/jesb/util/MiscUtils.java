package com.otk.jesb.util;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.otk.jesb.Expression;
import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.InMemoryCompiler;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.CompositeStep;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.JAR;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.CompositeStep.CompositeStepMetadata;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.ui.GUI;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.javabean.BeanProvider;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ClassUtils;

public class MiscUtils {

	public static final String SERIALIZED_FILE_NAME_SUFFIX = ".jesb.xml";
	public static InMemoryCompiler IN_MEMORY_COMPILER = new InMemoryCompiler();
	public static final Pattern SPECIAL_REGEX_CHARS_PATTERN = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
	public static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
	public static final String[] NEW_LINE_SEQUENCES = new String[] { "\r\n", "\n", "\r" };

	public static final XStream XSTREAM = new XStream() {
		@Override
		protected MapperWrapper wrapMapper(MapperWrapper next) {
			return new MapperWrapper(next) {
				@Override
				public String serializedClass(@SuppressWarnings("rawtypes") Class type) {
					if (type.isAnonymousClass()) {
						throw new UnexpectedError("Cannot serialize instance of class " + type
								+ ": Anonymous class instance serialization forbidden");
					}
					return super.serializedClass(type);
				}

			};
		}
	};
	static {
		XSTREAM.registerConverter(new JavaBeanConverter(XSTREAM.getMapper(), new BeanProvider() {
			@Override
			protected boolean canStreamProperty(PropertyDescriptor descriptor) {
				final boolean canStream = super.canStreamProperty(descriptor);
				if (!canStream) {
					return false;
				}
				final boolean readMethodIsTransient = descriptor.getReadMethod() == null
						|| descriptor.getReadMethod().getAnnotation(Transient.class) != null;
				final boolean writeMethodIsTransient = descriptor.getWriteMethod() == null
						|| descriptor.getWriteMethod().getAnnotation(Transient.class) != null;
				final boolean isTransient = readMethodIsTransient || writeMethodIsTransient;

				return !isTransient;
			}

			@Override
			public void writeProperty(Object object, String propertyName, Object value) {
				if (!propertyWriteable(propertyName, object.getClass())) {
					return;
				}
				super.writeProperty(object, propertyName, value);
			}
		}), XStream.PRIORITY_VERY_LOW);
		XSTREAM.registerConverter(new ReflectionConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()) {
			@Override
			public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
				if ((type != null) && AbstractSimpleCustomizableFieldControlPlugin.AbstractConfiguration.class
						.isAssignableFrom(type)) {
					return true;
				}
				return false;
			}
		}, XStream.PRIORITY_VERY_HIGH);
		XSTREAM.addPermission(AnyTypePermission.ANY);
		XSTREAM.ignoreUnknownElements();
	}
	private static final String SERIALIZATION_CHARSET_NAME = "UTF-8";
	private static final WeakHashMap<Object, String> DIGITAL_UNIQUE_IDENTIFIER_CACHE = new WeakHashMap<Object, String>();
	private static final Object DIGITAL_UNIQUE_IDENTIFIER_CACHE_MUTEX = new Object();

	public static void sleepSafely(long durationMilliseconds) {
		try {
			Thread.sleep(durationMilliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static String escapeRegex(String str) {
		return SPECIAL_REGEX_CHARS_PATTERN.matcher(str).replaceAll("\\\\$0");
	}

	public static String escapeJavaString(String s) {
		return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'") // <== not necessary
				.replace("\"", "\\\"");
	}

	public static String escapeHTML(String string, boolean convertNewLinesToHTML) {
		StringBuffer sb = new StringBuffer(string.length());
		int len = string.length();
		char currentC;
		char lastC = 0;
		for (int i = 0; i < len; i++) {
			currentC = string.charAt(i);
			currentC = MiscUtils.standardizeNewLineSequences(lastC, currentC);
			if (currentC == '"')
				sb.append("&quot;");
			else if (currentC == '&')
				sb.append("&amp;");
			else if (currentC == '<')
				sb.append("&lt;");
			else if (currentC == '>')
				sb.append("&gt;");
			else if (currentC == '\n')
				// Handle Newline
				if (convertNewLinesToHTML) {
					sb.append("<BR>");
				} else {
					sb.append(currentC);
				}
			else {
				int ci = 0xffff & currentC;
				if (ci < 160)
					// nothing special only 7 Bit
					sb.append(currentC);
				else {
					// Not 7 Bit use the unicode system
					sb.append("&#");
					sb.append(new Integer(ci).toString());
					sb.append(';');
				}
			}
			lastC = currentC;
		}
		return sb.toString();
	}

	public static String getDigitalUniqueIdentifier() {
		return String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
	}

	public static String toDigitalUniqueIdentifier(Object object) {
		synchronized (DIGITAL_UNIQUE_IDENTIFIER_CACHE_MUTEX) {
			String result = DIGITAL_UNIQUE_IDENTIFIER_CACHE.get(object);
			if (result == null) {
				result = getDigitalUniqueIdentifier();
				DIGITAL_UNIQUE_IDENTIFIER_CACHE.put(object, result);
			}
			return result;
		}
	}

	public static Object fromFromDigitalUniqueIdentifier(String digitalUniqueIdentifier) {
		synchronized (DIGITAL_UNIQUE_IDENTIFIER_CACHE_MUTEX) {
			List<Object> keys = MiscUtils.getKeysFromValue(DIGITAL_UNIQUE_IDENTIFIER_CACHE, digitalUniqueIdentifier);
			if (keys.size() != 1) {
				throw new UnexpectedError();
			}
			return keys.get(0);
		}
	}

	public static <E> Iterable<E> secureIterable(Iterable<E> iterable) {
		ArrayList<E> list = new ArrayList<E>();
		for (E item : iterable) {
			list.add(item);
		}
		return list;
	}

	public static boolean negate(boolean b) {
		return !b;
	}

	public static ResourcePath getIconImagePath(Step step) {
		for (CompositeStepMetadata metadata : GUI.BUILTIN_COMPOSITE_STEP_METADATAS) {
			if (metadata.getCompositeStepClass().equals(step.getClass())) {
				return metadata.getCompositeStepIconImagePath();
			}
		}
		OperationBuilder<?> operationBuilder = step.getOperationBuilder();
		if (operationBuilder == null) {
			return null;
		}
		for (OperationMetadata<?> metadata : MiscUtils.getAllOperationMetadatas()) {
			if (metadata.getOperationBuilderClass().equals(operationBuilder.getClass())) {
				return metadata.getOperationIconImagePath();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Asset> List<T> findResources(Solution solution, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Asset resource : solution.getContents()) {
			if (resource.getClass().equals(resourceClass)) {
				result.add((T) resource);
			}
			if (resource instanceof Folder) {
				result.addAll(findDescendantResources((Folder) resource, resourceClass));
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Asset> List<T> findDescendantResources(Folder folder, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Asset resource : folder.getContents()) {
			if (resource.getClass().equals(resourceClass)) {
				result.add((T) resource);
			}
			if (resource instanceof Folder) {
				result.addAll(findDescendantResources((Folder) resource, resourceClass));
			}
		}
		return result;
	}

	public static int convertPositionToIndex(String text, int line, int column) {
		if (line < 1 || column < 1) {
			throw new IllegalArgumentException("Line and column numbers must be >= 1");
		}
		int index = 0;
		int currentLine = 1;
		int currentColumn = 1;
		for (char c : text.toCharArray()) {
			if (currentLine == line && currentColumn == column) {
				return index;
			}

			index++;

			if (c == '\n') {
				currentLine++;
				currentColumn = 1;
			} else {
				currentColumn++;
			}
		}
		if (currentLine == line && currentColumn == column) {
			return index;
		} else {
			throw new IllegalArgumentException("Line or column out of range");
		}
	}

	public static Pair<Integer, Integer> convertIndexToPosition(String text, int index) {
		if (index < 0 || index > text.length()) {
			throw new IllegalArgumentException("Index out of bounds");
		}

		int line = 0;
		int column = 0;

		for (int i = 0; i < index; i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				line++;
				column = 0;
			} else {
				column++;
			}
		}

		return new Pair<Integer, Integer>(line, column);
	}

	public static Point getRectangleBorderContactOfLineToExternalPoint(int rectangleCenterX, int rectangleCenterY,
			int rectangleWidth, int rectangleHeight, int externalPointX, int externalPointY) {
		int x = externalPointX;
		int y = externalPointY;
		int minX = rectangleCenterX - Math.round(rectangleWidth / 2f);
		int minY = rectangleCenterY - Math.round(rectangleHeight / 2f);
		int maxX = rectangleCenterX + Math.round(rectangleWidth / 2f);
		int maxY = rectangleCenterY + Math.round(rectangleHeight / 2f);
		if ((minX < x && x < maxX) && (minY < y && y < maxY)) {
			return null;
		}
		float midX = (minX + maxX) / 2f;
		float midY = (minY + maxY) / 2f;
		// if (midX - x == 0) -> m == ±Inf -> minYx/maxYx == x (because value / ±Inf =
		// ±0)
		float m = (midY - y) / (midX - x);
		if (x <= midX) { // check "left" side
			float minXy = m * (minX - x) + y;
			if (minY <= minXy && minXy <= maxY) {
				return new Point(Math.round(minX), Math.round(minXy));
			}
		}
		if (x >= midX) { // check "right" side
			float maxXy = m * (maxX - x) + y;
			if (minY <= maxXy && maxXy <= maxY) {
				return new Point(Math.round(maxX), Math.round(maxXy));
			}
		}
		if (y <= midY) { // check "top" side
			float minYx = (minY - y) / m + x;
			if (minX <= minYx && minYx <= maxX) {
				return new Point(Math.round(minYx), Math.round(minY));
			}
		}
		if (y >= midY) { // check "bottom" side
			float maxYx = (maxY - y) / m + x;
			if (minX <= maxYx && maxYx <= maxX) {
				return new Point(Math.round(maxYx), Math.round(maxY));
			}
		}
		// edge case when finding midpoint intersection: m = 0/0 = NaN
		if (x == midX && y == midY) {
			return new Point(x, y);
		}
		// Should never happen :) If it does, please tell me!
		throw new UnexpectedError();
	}

	public static <T> String stringJoin(T[] array, String separator) {
		return stringJoin(Arrays.asList(array), separator);
	}

	public static String stringJoin(List<?> list, String separator) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Object item = list.get(i);
			if (i > 0) {
				result.append(separator);
			}
			if (item == null) {
				result.append("null");
			} else {
				result.append(item.toString());
			}
		}
		return result.toString();
	}

	public static <BASE, C extends BASE> List<BASE> convertCollection(Collection<C> ts) {
		List<BASE> result = new ArrayList<BASE>();
		for (C t : ts) {
			result.add((BASE) t);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <BASE, C extends BASE> List<C> convertCollectionUnsafely(Collection<BASE> bs) {
		List<C> result = new ArrayList<C>();
		for (BASE b : bs) {
			result.add((C) b);
		}
		return result;
	}

	public static String adaptClassNameToSourceCode(String className) {
		className = className.replace("$", ".");
		int arrayDimension = 0;
		String arrayComponentTypeName = className;
		while ((arrayComponentTypeName = getArrayComponentTypeName(arrayComponentTypeName)) != null) {
			arrayDimension++;
			className = arrayComponentTypeName;
		}
		for (int i = 0; i < arrayDimension; i++) {
			className += "[]";
		}
		return className;
	}

	public static String getArrayComponentTypeName(String className) {
		if (className.startsWith("[[")) {
			return className.substring(1);
		}
		if (className.startsWith("[L") && className.endsWith(";")) {
			return className.substring(2, className.length() - 1);
		}
		if (className.equals("[Z")) {
			return "boolean";
		}
		if (className.equals("[B")) {
			return "byte";
		}
		if (className.equals("[S")) {
			return "short";
		}
		if (className.equals("[I")) {
			return "int";
		}
		if (className.equals("[J")) {
			return "long";
		}
		if (className.equals("[F")) {
			return "float";
		}
		if (className.equals("[D")) {
			return "double";
		}
		if (className.equals("[C")) {
			return "char";
		}
		return null;
	}

	public static boolean isArrayTypeName(String className) {
		return getArrayComponentTypeName(className) != null;
	}

	public static Class<?> getArrayType(Class<?> componentType) {
		return Array.newInstance(componentType, 0).getClass();
	}

	public static String read(InputStream in) throws IOException {
		return new String(readBinary(in));
	}

	public static byte[] readBinary(InputStream in) throws IOException {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			return buffer.toByteArray();
		} catch (IOException e) {
			throw new IOException("Error while reading input stream: " + e.getMessage(), e);
		}
	}

	public static byte[] readBinary(File file) throws IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			return readBinary(in);
		}
	}

	public static void write(File file, String text, boolean append) throws IOException {
		writeBinary(file, text.getBytes(), append);
	}

	public static void writeBinary(File file, byte[] bytes, boolean append) throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file, append);
			out.write(bytes);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new IOException("Unable to write file : '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static File createTemporaryFile(String extension) throws IOException {
		return File.createTempFile("file-", "." + extension);
	}

	public static File createTemporaryDirectory() throws IOException {
		File result = File.createTempFile("directory-", ".tmp");
		delete(result);
		createDirectory(result);
		return result;
	}

	public static void createDirectory(File dir) throws IOException {
		createDirectory(dir, false);
	}

	public static void createDirectory(File dir, boolean createNonExistingAncestors) throws IOException {
		if (dir.isDirectory()) {
			throw new IOException("Cannot create directory: '" + dir + "': It already exists");
		}
		try {
			if (!(createNonExistingAncestors ? dir.mkdirs() : dir.mkdir())) {
				throw new IOException("System error");
			}
		} catch (Exception e) {
			throw new IOException("Failed to create directory: '" + dir + "': " + e.toString(), e);
		}
	}

	public static void delete(File file) throws IOException {
		delete(file, null, null);
	}

	public static void delete(File file, FilenameFilter filter, Listener<Pair<File, Exception>> errorHandler)
			throws IOException {
		if (file.isDirectory()) {
			for (File childFile : file.listFiles(filter)) {
				delete(childFile, filter, errorHandler);
			}
			if (file.listFiles().length > 0) {
				return;
			}
		}
		boolean success;
		try {
			success = file.delete();
			if (!success) {
				throw new IOException("System error");
			}
		} catch (IOException e) {
			e = new IOException(
					"Failed to delete file system resource: '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
			if (errorHandler != null) {
				errorHandler.handle(new Pair<File, Exception>(file, e));
			} else {
				throw e;
			}
		}
	}

	public static String extractSimpleNameFromClassName(String className) {
		if (!isPackageNameInClassName(className)) {
			return className;
		}
		return className.substring(className.lastIndexOf(".") + 1);
	}

	public static String extractPackageNameFromClassName(String className) {
		if (!isPackageNameInClassName(className)) {
			return null;
		}
		return className.substring(0, className.lastIndexOf("."));
	}

	public static boolean isPackageNameInClassName(String className) {
		return className.contains(".");
	}

	public static <K, V> K getFirstKeyFromValue(Map<K, V> map, V value) {
		List<K> list = getKeysFromValue(map, value);
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	public static <K, V> List<K> getKeysFromValue(Map<K, V> map, Object value) {
		List<K> result = new ArrayList<K>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			if (MiscUtils.equalsOrBothNull(entry.getValue(), value)) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	public static boolean equalsOrBothNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equals(o2);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T copy(T object) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			serialize(object, output);
			ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
			return (T) deserialize(input);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	public static void serialize(Object object, OutputStream output) throws IOException {
		try {
			XSTREAM.toXML(object, new OutputStreamWriter(output, SERIALIZATION_CHARSET_NAME));
		} catch (XStreamException e) {
			throw new IOException(e);
		}
	}

	public static Object deserialize(InputStream input) throws IOException {
		try {
			return XSTREAM.fromXML(new InputStreamReader(input, SERIALIZATION_CHARSET_NAME));
		} catch (XStreamException e) {
			throw new IOException(e);
		}
	}

	public static String serialize(Object object) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			serialize(object, output);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
		try {
			return output.toString(SERIALIZATION_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new UnexpectedError(e);
		}
	}

	public static Object deserialize(String inputString) {
		ByteArrayInputStream input;
		try {
			input = new ByteArrayInputStream(inputString.getBytes(SERIALIZATION_CHARSET_NAME));
		} catch (UnsupportedEncodingException e) {
			throw new UnexpectedError(e);
		}
		try {
			return deserialize(input);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	public static String getPrintedStackTrace(Throwable t) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(out));
		return out.toString();
	}

	public static DateFormat getDefaultDateFormat() {
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
	}

	public static Date now() {
		return new Date();
	}

	public static char standardizeNewLineSequences(char lastC, char c) {
		for (String newLineSequence : NEW_LINE_SEQUENCES) {
			if (newLineSequence.equals("" + lastC + c)) {
				return 0;
			}
		}
		for (String newLineSequence : NEW_LINE_SEQUENCES) {
			if (newLineSequence.startsWith("" + c)) {
				return '\n';
			}
		}
		return c;
	}

	public static <K, V> void add(LinkedHashMap<K, V> linkedHashMap, int index, K key, V value) {
		List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(linkedHashMap.entrySet());
		list.add(index, new Map.Entry<K, V>() {

			@Override
			public K getKey() {
				return key;
			}

			@Override
			public V getValue() {
				return value;
			}

			@Override
			public V setValue(V value) {
				throw new UnsupportedOperationException();
			}
		});
		linkedHashMap.clear();
		for (Map.Entry<K, V> entry : list) {
			linkedHashMap.put(entry.getKey(), entry.getValue());
		}
	}

	public static <T> List<T> getReverse(List<T> ts) {
		List<T> result = new ArrayList<T>(ts);
		Collections.reverse(result);
		return result;
	}

	public static List<Step> getDescendants(CompositeStep<?> compositeStep, Plan plan) {
		List<Step> result = new ArrayList<Step>();
		for (Step childStep : compositeStep.getChildren(plan)) {
			result.add(childStep);
			if (childStep instanceof CompositeStep) {
				result.addAll(getDescendants((CompositeStep<?>) childStep, plan));
			}
		}
		return result;
	}

	public static String nextNumbreredName(String name) {
		final String NUMBERED_NAME_PATTERN = "^(.*)([0-9]+)$";
		if (!name.matches(NUMBERED_NAME_PATTERN)) {
			return name + "1";
		} else {
			int number = Integer.valueOf(name.replaceAll(NUMBERED_NAME_PATTERN, "$2"));
			return name.replaceAll(NUMBERED_NAME_PATTERN, "$1") + (number + 1);
		}
	}

	public static <T> List<T> added(List<T> ts, int index, T newItem) {
		List<T> result = new ArrayList<T>(ts);
		if (index == -1) {
			result.add(newItem);
		} else {
			result.add(index, newItem);
		}
		return result;
	}

	public static <T> List<T> removed(List<T> ts, int index, T oldItem) {
		List<T> result = new ArrayList<T>(ts);
		if (index == -1) {
			if (!result.remove(oldItem)) {
				throw new NoSuchElementException();
			}
		} else {
			if (!oldItem.equals(result.remove(index))) {
				throw new NoSuchElementException();
			}
		}
		return result;
	}

	public static void improveRenderingQuality(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	public static <T> CompiledFunction compileExpression(String expressionString,
			List<VariableDeclaration> variableDeclarations, Class<T> returnType) throws CompilationError {
		Expression<T> expression = new Expression<T>(returnType);
		expression.set(expressionString);
		return expression.compile(variableDeclarations);
	}

	public static boolean areIncompatible(Class<?> class1, Class<?> class2) {
		if ((class2.isPrimitive() ? ClassUtils.primitiveToWrapperClass(class2) : class2)
				.isAssignableFrom((class1.isPrimitive() ? ClassUtils.primitiveToWrapperClass(class1) : class1))) {
			return false;
		}
		if ((class1.isPrimitive() ? ClassUtils.primitiveToWrapperClass(class1) : class1)
				.isAssignableFrom((class2.isPrimitive() ? ClassUtils.primitiveToWrapperClass(class2) : class2))) {
			return false;
		}
		return true;
	}

	public static int positionAfterReplacement(int positionBefore, String inputString, String target,
			String replacement) {
		if (target.isEmpty() || positionBefore < 0 || positionBefore > inputString.length()) {
			return positionBefore;
		}
		int newIndex = positionBefore;
		int i = 0;
		int shift = 0;
		while (i < inputString.length()) {
			int found = inputString.indexOf(target, i);
			if (found == -1) {
				break;
			}
			int end = found + target.length();
			if (positionBefore >= found && positionBefore < end) {
				// The index falls inside a replaced segment — it no longer maps to any
				// position.
				return -1;
			}
			if (found >= positionBefore) {
				// No need to process further if the replacement occurs after the original
				// index.
				break;
			}
			// Accumulate the change in length caused by this replacement.
			shift += replacement.length() - target.length();
			i = end;
		}
		return newIndex + shift;
	}

	public static OutputStream unifyOutputStreams(final OutputStream... outputStreams) {
		return new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				for (OutputStream out : outputStreams) {
					out.write(b);
				}
			}

			@Override
			public void write(byte[] b) throws IOException {
				for (OutputStream out : outputStreams) {
					out.write(b);
				}
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				for (OutputStream out : outputStreams) {
					out.write(b, off, len);
				}
			}

			@Override
			public void flush() throws IOException {
				for (OutputStream out : outputStreams) {
					out.flush();
				}
			}

			@Override
			public void close() throws IOException {
				for (OutputStream out : outputStreams) {
					out.close();
				}
			}
		};
	}

	public static String truncateNicely(String s, int length) {
		if (s.length() > length) {
			s = s.substring(0, length - 3) + "...";
		}
		return s;
	}

	public static Class<?> getJESBClass(String typeName) {
		String arrayComponentTypeName = getArrayComponentTypeName(typeName);
		if (arrayComponentTypeName != null) {
			return getArrayType(getJESBClass(arrayComponentTypeName));
		}
		if (ClassUtils.PRIMITIVE_CLASS_BY_NAME.containsKey(typeName)) {
			return ClassUtils.PRIMITIVE_CLASS_BY_NAME.get(typeName);
		}
		try {
			return IN_MEMORY_COMPILER.loadClassThroughCache(typeName);
		} catch (ClassNotFoundException e1) {
			throw new PotentialError(e1);
		}
	}

	public static Class<?> getJESBClassFromCanonicalName(String canonicalName) {
		try {
			return getJESBClass(canonicalName);
		} catch (PotentialError e1) {
			try {
				int lastDotIndex = canonicalName.lastIndexOf('.');
				if (lastDotIndex == -1) {
					throw new PotentialError(new UnexpectedError());
				}
				return getJESBClassFromCanonicalName(
						canonicalName.substring(0, lastDotIndex) + "$" + canonicalName.substring(lastDotIndex + 1));
			} catch (PotentialError e2) {
				throw new PotentialError(new ClassNotFoundException("Canonical name: " + canonicalName));
			}
		}
	}

	public static void relieveCPU() {
		sleepSafely(100);
	}

	public static Thread redirectStream(final InputStream src, final OutputStream dst, String reason) {
		Thread thread = new Thread("StreamRedirector (" + reason + ")") {
			public void run() {
				try {
					while (true) {
						if (src.available() > 0) {
							int b = src.read();
							if (b == -1) {
								break;
							}
							dst.write(b);
						} else {
							if (isInterrupted()) {
								break;
							} else {
								MiscUtils.relieveCPU();
							}
						}
					}
				} catch (IOException e) {
					return;
				}
			}
		};
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
		return thread;
	}

	public static List<Class<?>> expandWithEnclosedClasses(List<Class<?>> classes) {
		classes = new ArrayList<Class<?>>(classes);
		List<Class<?>> innerClasses = null;
		{
			while (true) {
				innerClasses = ((innerClasses == null) ? classes : innerClasses).stream().flatMap(clazz -> {
					List<Class<?>> result = new ArrayList<Class<?>>();
					result.addAll(Arrays.asList(clazz.getDeclaredClasses()));
					result.addAll(getDeclaredAnonymousClasses(clazz));
					return result.stream();
				}).collect(Collectors.toList());
				if (innerClasses.size() == 0) {
					break;
				}
				classes.addAll(innerClasses);
			}
		}
		return classes;
	}

	public static List<Class<?>> getDeclaredAnonymousClasses(Class<?> clazz) {
		List<Class<?>> result = new ArrayList<Class<?>>();
		int i = 1;
		while (true) {
			try {
				result.add(clazz.getClassLoader().loadClass(clazz.getName() + "$" + i));
			} catch (ClassNotFoundException e) {
				break;
			}
			i++;
		}
		return result;
	}

	public static Class<?> inferOperationClass(Class<?> operationBuilderClass) {
		try {
			return operationBuilderClass.getMethod("build", ExecutionContext.class, ExecutionInspector.class)
					.getReturnType();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new UnexpectedError(e);
		}
	}

	public static Class<? extends OperationBuilder<?>> findOperationBuilderClass(
			Class<? extends Operation> operationCass) {
		for (OperationMetadata<?> metadata : MiscUtils.getAllOperationMetadatas()) {
			if (inferOperationClass(metadata.getOperationBuilderClass()) == operationCass) {
				return metadata.getOperationBuilderClass();
			}
		}
		return null;
	}

	public static List<OperationMetadata<?>> getAllOperationMetadatas() {
		List<OperationMetadata<?>> result = new ArrayList<OperationMetadata<?>>();
		result.addAll(GUI.BUILTIN_OPERATION_METADATAS);
		result.addAll(JAR.PLUGIN_OPERATION_METADATAS);
		return result;
	}

	public static List<ActivatorMetadata> getAllActivatorMetadatas() {
		List<ActivatorMetadata> result = new ArrayList<ActivatorMetadata>();
		result.addAll(GUI.BUILTIN_ACTIVATOR__METADATAS);
		result.addAll(JAR.PLUGIN_ACTIVATOR_METADATAS);
		return result;
	}

	public static List<ResourceMetadata> getAllResourceMetadatas() {
		List<ResourceMetadata> result = new ArrayList<ResourceMetadata>();
		result.addAll(GUI.BUILTIN_RESOURCE_METADATAS);
		result.addAll(JAR.PLUGIN_RESOURCE_METADATAS);
		return result;
	}

}
