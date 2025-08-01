package com.otk.jesb.util;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.otk.jesb.CompositeStep;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.InMemoryCompiler;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.ui.JESBReflectionUI;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.BeanProvider;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.util.ClassUtils;

public class MiscUtils {

	private static final XStream XSTREAM = new XStream() {
		@Override
		protected MapperWrapper wrapMapper(MapperWrapper next) {
			return new MapperWrapper(next) {
				@Override
				public String serializedClass(@SuppressWarnings("rawtypes") Class type) {
					if (type.isAnonymousClass()) {
						throw new UnexpectedError("Cannot serialize instance of forbidden anonymous class " + type);
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
		}), -20);
		XSTREAM.addPermission(AnyTypePermission.ANY);
		XSTREAM.ignoreUnknownElements();
	}
	private static final String SERIALIZATION_CHARSET_NAME = "UTF-8";
	private static final WeakHashMap<Object, String> DIGITAL_UNIQUE_IDENTIFIER_CACHE = new WeakHashMap<Object, String>();
	private static final Object DIGITAL_UNIQUE_IDENTIFIER_CACHE_MUTEX = new Object();

	public static InMemoryCompiler IN_MEMORY_COMPILER = new InMemoryCompiler();
	static {
		MiscUtils.IN_MEMORY_COMPILER.setOptions(Arrays.asList("-parameters"));
	}
	public static final Pattern SPECIAL_REGEX_CHARS_PATTERN = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
	public static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
	public static final String[] NEW_LINE_SEQUENCES = new String[] { "\r\n", "\n", "\r" };

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
		OperationBuilder operationBuilder = step.getOperationBuilder();
		if (operationBuilder == null) {
			return null;
		}
		for (OperationMetadata operationMetadata : JESBReflectionUI.OPERATION_METADATAS) {
			if (operationMetadata.getOperationBuilderClass().equals(operationBuilder.getClass())) {
				return operationMetadata.getOperationIconImagePath();
			}
		}
		for (OperationMetadata operationMetadata : JESBReflectionUI.COMPOSITE_METADATAS) {
			if (operationMetadata.getOperationBuilderClass().equals(operationBuilder.getClass())) {
				return operationMetadata.getOperationIconImagePath();
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

	public static File createTemporaryDirectory() throws Exception {
		File result = File.createTempFile("directory-", ".tmp");
		delete(result);
		createDirectory(result);
		return result;
	}

	public static void createDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			return;
		}
		try {
			if (!dir.mkdir()) {
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
		XSTREAM.toXML(object, new OutputStreamWriter(output, SERIALIZATION_CHARSET_NAME));
	}

	public static Object deserialize(InputStream input) throws IOException {
		return XSTREAM.fromXML(new InputStreamReader(input, SERIALIZATION_CHARSET_NAME));
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

	public static List<Step> getDescendants(CompositeStep compositeStep, Plan plan) {
		List<Step> result = new ArrayList<Step>();
		for (Step childStep : compositeStep.getChildren(plan)) {
			result.add(childStep);
			if (childStep instanceof CompositeStep) {
				result.addAll(getDescendants((CompositeStep) childStep, plan));
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

	public static void improveRenderingQuality(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	public static CompiledFunction compileExpression(String expression, List<VariableDeclaration> variableDeclarations,
			Class<?> returnType) throws CompilationError {
		try {
			return CompiledFunction.get("return " + expression + ";", variableDeclarations, returnType);
		} catch (CompilationError e) {
			String sourceCode = expression;
			int startPosition = (e.getStartPosition() != -1) ? (e.getStartPosition() - "return ".length())
					: e.getStartPosition();
			int endPosition = (e.getEndPosition() != -1) ? (e.getEndPosition() - "return ".length())
					: e.getEndPosition();
			throw new CompilationError(startPosition, endPosition, e.getMessage(), null, sourceCode);
		}
	}

	public static ITypeInfo getInfoFromResolvedType(ResolvedType resolvedType) {
		if (resolvedType.isPrimitive()) {
			return TypeInfoProvider.getTypeInfo(ClassUtils
					.wrapperToPrimitiveClass(TypeInfoProvider.getClass(resolvedType.asPrimitive().getBoxTypeQName())));
		} else if (resolvedType.isReferenceType()) {
			ResolvedReferenceType referenceType = resolvedType.asReferenceType();
			String qualifiedName = referenceType.getQualifiedName();
			Class<?> javaType = TypeInfoProvider.getClassFromCanonicalName(qualifiedName);
			List<ResolvedType> typeParameters = referenceType.typeParametersValues();
			if (typeParameters.size() > 0) {
				List<Class<?>> typeParameterClasses = new ArrayList<Class<?>>();
				for (ResolvedType resolvedTypeParameter : typeParameters) {
					ITypeInfo typeParameterInfo = getInfoFromResolvedType(resolvedTypeParameter);
					if (typeParameterInfo == null) {
						return TypeInfoProvider.getTypeInfo(javaType);
					}
					typeParameterClasses.add(((DefaultTypeInfo) typeParameterInfo).getJavaType());
				}
				return TypeInfoProvider.getTypeInfo(javaType,
						typeParameterClasses.toArray(new Class<?>[typeParameterClasses.size()]));
			} else {
				return TypeInfoProvider.getTypeInfo(javaType);
			}
		} else if (resolvedType.isArray()) {
			Class<?> componentClass = ((DefaultTypeInfo) getInfoFromResolvedType(
					resolvedType.asArrayType().getComponentType())).getJavaType();
			return TypeInfoProvider.getTypeInfo(Array.newInstance(componentClass, 0).getClass());
		} else if (resolvedType.isWildcard() && resolvedType.asWildcard().isBounded()) {
			return getInfoFromResolvedType(resolvedType.asWildcard().getBoundedType());
		} else {
			return null;
		}
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

}
