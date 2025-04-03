package com.otk.jesb.util;

import java.awt.Point;
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
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.otk.jesb.Asset;
import com.otk.jesb.Folder;
import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Solution;
import com.otk.jesb.Step;
import com.otk.jesb.Structure.Structured;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.InMemoryJavaCompiler;
import com.otk.jesb.instantiation.EnumerationItemSelector;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.Function.CompilationContext;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.MapEntryBuilder;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.ui.JESBReflectionUI;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class MiscUtils {

	public static InMemoryJavaCompiler IN_MEMORY_JAVA_COMPILER = new InMemoryJavaCompiler();
	static {
		MiscUtils.IN_MEMORY_JAVA_COMPILER.setOptions(Arrays.asList("-parameters"));
	}
	public static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

	private static final String PARENT_STRUCTURE_TYPE_NAME_SYMBOL = "${..}";

	public static String escapeRegex(String str) {
		return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
	}

	public static Object executeFunction(Function function, EvaluationContext evaluationContext) throws Exception {
		ExecutionContext executionContext = evaluationContext.getExecutionContext();
		Plan currentPlan = executionContext.getPlan();
		Step currentStep = executionContext.getCurrentStep();
		Plan.ValidationContext validationContext = currentPlan.getValidationContext(currentStep);
		CompilationContext compilationContext = (currentStep != null)
				? currentStep.getActivityBuilder().findFunctionCompilationContext(function, validationContext)
				: currentPlan.getOutputBuilder().getFacade().findFunctionCompilationContext(function,
						validationContext);
		if (!MiscUtils.equalsOrBothNull(compilationContext.getVerificationContext().getParentFacade(),
				evaluationContext.getParentFacade())) {
			throw new AssertionError();
		}
		if (!Arrays.equals(
				evaluationContext.getExecutionContext().getVariables().stream().map(variable -> variable.getName())
						.toArray(),
				compilationContext.getVerificationContext().getValidationContext().getVariableDeclarations().stream()
						.map(variableDeclaration -> variableDeclaration.getVariableName()).toArray())) {
			throw new AssertionError();
		}
		CompiledFunction compiledFunction = CompiledFunction.get(
				makeTypeNamesAbsolute(function.getFunctionBody(),
						getAncestorStructureInstanceBuilders(
								compilationContext.getVerificationContext().getParentFacade())),
				compilationContext.getVerificationContext().getValidationContext(),
				compilationContext.getFunctionReturnType());
		return compiledFunction.execute(executionContext);
	}

	public static void validateFunction(String functionBody, CompilationContext context) throws CompilationError {
		CompiledFunction.get(
				makeTypeNamesAbsolute(functionBody,
						getAncestorStructureInstanceBuilders(context.getVerificationContext().getParentFacade())),
				context.getVerificationContext().getValidationContext(), context.getFunctionReturnType());
	}

	public static boolean isComplexType(ITypeInfo type) {
		Class<?> clazz = ((JavaTypeInfoSource) type.getSource()).getJavaType();
		if (ClassUtils.isPrimitiveClassOrWrapperOrString(clazz)) {
			return false;
		}
		if (type instanceof IEnumerationTypeInfo) {
			return false;
		}
		return true;
	}

	public static ValueMode getValueMode(Object value) {
		if (value instanceof Function) {
			return ValueMode.FUNCTION;
		} else {
			return ValueMode.PLAIN;
		}
	}

	public static AbstractConstructorInfo getConstructorInfo(ITypeInfo typeInfo, String selectedConstructorSignature) {
		if (selectedConstructorSignature == null) {
			if (typeInfo.getConstructors().size() == 0) {
				return null;
			} else {
				return (AbstractConstructorInfo) listSortedConstructors(typeInfo).get(0);
			}
		} else {
			return (AbstractConstructorInfo) ReflectionUIUtils.findMethodBySignature(typeInfo.getConstructors(),
					selectedConstructorSignature);
		}

	}

	public static List<IMethodInfo> listSortedConstructors(ITypeInfo typeInfo) {
		List<IMethodInfo> ctors = typeInfo.getConstructors();
		ctors = new ArrayList<IMethodInfo>(ctors);
		Collections.sort(ctors, new Comparator<IMethodInfo>() {
			@Override
			public int compare(IMethodInfo o1, IMethodInfo o2) {
				return Integer.valueOf(o1.getParameters().size()).compareTo(Integer.valueOf(o2.getParameters().size()));
			}
		});
		return ctors;
	}

	public static boolean isConditionFullfilled(Function condition, EvaluationContext context) throws Exception {
		if (condition == null) {
			return true;
		}
		Object conditionResult = MiscUtils.interpretValue(condition,
				TypeInfoProvider.getTypeInfo(Boolean.class.getName()), context);
		if (!(conditionResult instanceof Boolean)) {
			throw new AssertionError("Condition evaluation result is not boolean: '" + conditionResult + "'");
		}
		return (Boolean) conditionResult;
	}

	public static String escapeJavaString(String s) {
		return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'") // <== not necessary
				.replace("\"", "\\\"");
	}

	public static String express(Object value) {
		if (value instanceof Function) {
			return ((Function) value).getFunctionBody();
		} else if (value instanceof InstanceBuilder) {
			return null;
		} else if (value instanceof EnumerationItemSelector) {
			return ((EnumerationItemSelector) value).getSelectedItemName();
		} else {
			if (value == null) {
				return null;
			} else if (value instanceof String) {
				return "\"" + escapeJavaString((String) value) + "\"";
			} else {
				return value.toString();
			}
		}
	}

	public static Object interpretValue(Object value, ITypeInfo type, EvaluationContext context) throws Exception {
		if (value instanceof Function) {
			Object result = MiscUtils.executeFunction(((Function) value), context);
			if (!type.supports(result)) {
				throw new Exception(
						"Invalid function result '" + result + "': Expected value of type <" + type.getName() + ">");
			}
			return result;
		} else if (value instanceof InstanceBuilder) {
			Object result = ((InstanceBuilder) value).build(context);
			if (!type.supports(result)) {
				throw new Exception("Invalid instance builder result '" + result + "': Expected value of type <"
						+ type.getName() + ">");
			}
			return result;
		} else if (value instanceof EnumerationItemSelector) {
			for (Object item : ((IEnumerationTypeInfo) type).getValues()) {
				if (((IEnumerationTypeInfo) type).getValueInfo(item).getName()
						.equals(((EnumerationItemSelector) value).getSelectedItemName())) {
					return item;
				}
			}
			throw new AssertionError();
		} else {
			return value;
		}
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, Facade currentFacade) {
		return getDefaultInterpretableValue(type, ValueMode.PLAIN, currentFacade);
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, ValueMode valueMode, Facade currentFacade) {
		if (type == null) {
			return null;
		} else if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (!MiscUtils.isComplexType(type)
					&& !ClassUtils.isPrimitiveWrapperClass(((JavaTypeInfoSource) type.getSource()).getJavaType())) {
				Object defaultValue = ReflectionUIUtils.createDefaultInstance(type);
				if (defaultValue.getClass().isEnum()) {
					functionBody = "return "
							+ makeTypeNamesRelative(type.getName(), getAncestorStructureInstanceBuilders(currentFacade))
							+ "." + defaultValue.toString() + ";";
				} else if (defaultValue instanceof String) {
					functionBody = "return \"" + defaultValue + "\";";
				} else {
					functionBody = "return " + String.valueOf(defaultValue) + ";";
				}
			} else {
				functionBody = "return null;";
			}
			return new Function(functionBody);
		} else if (valueMode == ValueMode.PLAIN) {
			if ((currentFacade instanceof ParameterInitializerFacade) && (((ParameterInitializerFacade) currentFacade)
					.getCurrentInstanceBuilderFacade() instanceof RootInstanceBuilderFacade)) {
				RootInstanceBuilder rootInstanceBuilder = ((RootInstanceBuilderFacade) ((ParameterInitializerFacade) currentFacade)
						.getCurrentInstanceBuilderFacade()).getUnderlying();
				InstanceBuilder result = new InstanceBuilder();
				result.setTypeName(rootInstanceBuilder.getRootInstanceTypeName());
				result.setDynamicTypeNameAccessor(rootInstanceBuilder.getRootInstanceDynamicTypeNameAccessor());
				return result;
			}
			if (!MiscUtils.isComplexType(type)) {
				if (type instanceof IEnumerationTypeInfo) {
					EnumerationItemSelector result = new EnumerationItemSelector();
					result.configure((IEnumerationTypeInfo) type);
					if (result.getItemNames().size() > 0) {
						result.setSelectedItemName(result.getItemNames().get(0));
					}
					return result;
				} else {
					return ReflectionUIUtils.createDefaultInstance(type);
				}
			} else {
				if (type instanceof IMapEntryTypeInfo) {
					return new MapEntryBuilder();
				} else {
					return new InstanceBuilder(
							makeTypeNamesRelative(type.getName(), getAncestorStructureInstanceBuilders(currentFacade)));
				}
			}
		} else {
			return null;
		}
	}

	public static Object maintainInterpretableValue(Object value, ITypeInfo type) {
		if (value instanceof EnumerationItemSelector) {
			if (type instanceof IEnumerationTypeInfo) {
				((EnumerationItemSelector) value).configure((IEnumerationTypeInfo) type);
			}
		}
		return value;
	}

	public static String makeTypeNamesRelative(String text, List<InstanceBuilder> ancestorStructureInstanceBuilders) {
		if ((ancestorStructureInstanceBuilders == null) || (ancestorStructureInstanceBuilders.size() == 0)) {
			return text;
		}
		InstanceBuilder parentInstanceBuilder = ancestorStructureInstanceBuilders.get(0);
		String absoluteParentTypeName = parentInstanceBuilder.computeActualTypeName(
				ancestorStructureInstanceBuilders.subList(1, ancestorStructureInstanceBuilders.size()));
		return text.replace(absoluteParentTypeName, PARENT_STRUCTURE_TYPE_NAME_SYMBOL);
	}

	public static String makeTypeNamesAbsolute(String text, List<InstanceBuilder> ancestorStructureInstanceBuilders) {
		if ((ancestorStructureInstanceBuilders == null) || (ancestorStructureInstanceBuilders.size() == 0)) {
			return text;
		}
		InstanceBuilder parentInstanceBuilder = ancestorStructureInstanceBuilders.get(0);
		String absoluteParentTypeName = parentInstanceBuilder.computeActualTypeName(
				ancestorStructureInstanceBuilders.subList(1, ancestorStructureInstanceBuilders.size()));
		return text.replace(PARENT_STRUCTURE_TYPE_NAME_SYMBOL, absoluteParentTypeName);
	}

	public static String getDigitalUniqueIdentifier() {
		return String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
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
		ActivityBuilder activityBuilder = step.getActivityBuilder();
		if (activityBuilder == null) {
			return null;
		}
		for (ActivityMetadata activityMetadata : JESBReflectionUI.ACTIVITY_METADATAS) {
			if (activityMetadata.getActivityBuilderClass().equals(step.getActivityBuilder().getClass())) {
				return activityMetadata.getActivityIconImagePath();
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
		throw new AssertionError();
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
		return className.replace("$", ".");
	}

	public static List<InstanceBuilder> getAncestorStructureInstanceBuilders(Facade facade) {
		if (facade == null) {
			return null;
		}
		return Facade.getAncestors(facade).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade) && Structured.class
						.isAssignableFrom(((DefaultTypeInfo) ((InstanceBuilderFacade) f).getTypeInfo()).getJavaType()))
				.map(f -> ((InstanceBuilderFacade) f).getUnderlying()).collect(Collectors.toList());
	}

	public static String read(InputStream in) throws Exception {
		return new String(readBinary(in));
	}

	public static byte[] readBinary(InputStream in) throws Exception {
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
			throw new Exception("Error while reading input stream: " + e.getMessage(), e);
		}
	}

	public static void write(File file, String text, boolean append) throws Exception {
		writeBinary(file, text.getBytes(), append);
	}

	public static void writeBinary(File file, byte[] bytes, boolean append) throws Exception {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file, append);
			out.write(bytes);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new Exception("Unable to write file : '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}

	}

	public static File createTemporaryFile(String extension) throws Exception {
		return File.createTempFile("file-", "." + extension);
	}

	public static File createTemporaryDirectory() throws Exception {
		File result = File.createTempFile("directory-", ".tmp");
		delete(result);
		createDirectory(result);
		return result;
	}

	public static void createDirectory(File dir) throws Exception {
		if (dir.isDirectory()) {
			return;
		}
		try {
			if (!dir.mkdir()) {
				throw new Exception("System error");
			}
		} catch (Exception e) {
			throw new Exception("Failed to create directory: '" + dir + "': " + e.toString(), e);
		}
	}

	public static void delete(File file) throws Exception {
		delete(file, null, null);
	}

	public static void delete(File file, FilenameFilter filter, Listener<Pair<File, Exception>> errorHandler)
			throws Exception {
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
				throw new Exception("System error");
			}
		} catch (Exception e) {
			e = new Exception("Failed to delete resource: '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
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
			throw new AssertionError(e);
		}
	}

	public static Object deserialize(InputStream input) throws IOException {
		return getXStream().fromXML(new InputStreamReader(input, "UTF-8"));
	}

	public static void serialize(Object object, OutputStream output) throws IOException {
		getXStream().toXML(object, new OutputStreamWriter(output, "UTF-8"));
	}

	private static XStream getXStream() {
		XStream result = new XStream();
		result.registerConverter(new JavaBeanConverter(result.getMapper()), -20);
		result.addPermission(AnyTypePermission.ANY);
		result.ignoreUnknownElements();
		return result;
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

	public static final String[] NEW_LINE_SEQUENCES = new String[] { "\r\n", "\n", "\r" };

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
}
