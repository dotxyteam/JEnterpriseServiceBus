package com.otk.jesb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.IInfo;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.method.DefaultMethodInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntry;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PathExplorer {

	private String typeName;

	public PathExplorer(String typeName) {
		this.typeName = typeName;
	}

	public TypeNode getRootNode() {
		return new TypeNode(null, typeName);
	}

	public static interface PathNode {

		List<PathNode> getChildren();

	}

	public static class TypeNode implements PathNode {

		private PathNode parent;
		private String typeName;

		public TypeNode(PathNode parent, String typeName) {
			this.parent = parent;
			this.typeName = typeName;
		}

		public PathNode getParent() {
			return parent;
		}

		public String getTypeName() {
			return typeName;
		}

		public ITypeInfo getTypeInfo() {
			ITypeInfo result = TypeInfoProvider.getTypeInfo(typeName);
			if (result instanceof IListTypeInfo) {
				if (parent instanceof FieldNode) {
					FieldNode fieldNode = (FieldNode) parent;
					IFieldInfo listFieldInfo = fieldNode.getFieldInfo();
					result = TypeInfoProvider.getTypeInfo(typeName, listFieldInfo);
				}
			}
			if (result instanceof IMapEntryTypeInfo) {
				if (parent instanceof ListItemNode) {
					ListItemNode mapEntryNode = (ListItemNode) parent;
					result = (StandardMapEntryTypeInfo) mapEntryNode.getItemType();
				}
			}
			return result;
		}

		@Override
		public List<PathNode> getChildren() {
			List<PathNode> result = new ArrayList<PathNode>();
			ITypeInfo typeInfo = getTypeInfo();
			if (typeInfo instanceof IListTypeInfo) {
				result.add(new ListItemNode(this));
			} else {
				for (IFieldInfo field : typeInfo.getFields()) {
					result.add(new FieldNode(this, field.getName()));
				}
			}
			Collections.sort(result, new Comparator<PathNode>() {
				List<Class<?>> CLASSES_ORDER = Arrays.asList(FieldNode.class, ListItemNode.class);

				@Override
				public int compare(PathNode o1, PathNode o2) {
					if (!o1.getClass().equals(o2.getClass())) {
						return Integer.valueOf(CLASSES_ORDER.indexOf(o1.getClass()))
								.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(o2.getClass())));
					}
					if ((o1 instanceof FieldNode) && (o2 instanceof FieldNode)) {
						FieldNode fn1 = (FieldNode) o1;
						FieldNode fn2 = (FieldNode) o2;
						return fn1.getFieldName().compareTo(fn2.getFieldName());
					} else {
						throw new AssertionError();
					}
				}
			});
			return result;
		}

		@Override
		public String toString() {
			return "<" + typeName + ">";
		}

	}

	public static class MapEntryTypeNode extends TypeNode {

		public MapEntryTypeNode(PathNode parent) {
			super(parent, StandardMapEntry.class.getName());
		}

		@Override
		public String toString() {
			return "<MapEntry>";
		}

	}

	public static class FieldNode implements PathNode {

		private TypeNode parent;
		private String fieldName;

		public FieldNode(TypeNode parent, String fieldName) {
			this.parent = parent;
			this.fieldName = fieldName;
		}

		public TypeNode getParent() {
			return parent;
		}

		public String getFieldName() {
			return fieldName;
		}

		public IFieldInfo getFieldInfo() {
			return ReflectionUIUtils.findInfoByName(parent.getTypeInfo().getFields(), fieldName);
		}

		@Override
		public List<PathNode> getChildren() {
			ITypeInfo fieldTypeInfo = getFieldInfo().getType();
			if (!Utils.isComplexType(fieldTypeInfo)) {
				return Collections.emptyList();
			} else {
				return Collections.singletonList(new TypeNode(this, fieldTypeInfo.getName()));
			}
		}

		@Override
		public String toString() {
			return fieldName;
		}
	}

	public static class ListItemNode implements PathNode {

		private TypeNode parent;

		public ListItemNode(TypeNode parent) {
			this.parent = parent;
		}

		public TypeNode getParent() {
			return parent;
		}

		public ITypeInfo getItemType() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			return ((IListTypeInfo) parentTypeInfo).getItemType();
		}

		@Override
		public List<PathNode> getChildren() {
			ITypeInfo itemTypeInfo = getItemType();
			if (!Utils.isComplexType(itemTypeInfo)) {
				return Collections.emptyList();
			} else {
				if (itemTypeInfo instanceof IMapEntryTypeInfo) {
					return Collections.singletonList(new MapEntryTypeNode(this));
				} else {
					return Collections.singletonList(new TypeNode(this, itemTypeInfo.getName()));
				}
			}
		}

		@Override
		public String toString() {
			return "[i]";
		}
	}

	private static class TypeInfoProvider {

		public static ITypeInfo getTypeInfo(String typeName) {
			return getTypeInfo(typeName, null);
		}

		public static ITypeInfo getTypeInfo(String typeName, IInfo typeOwner) {
			Class<?> objectClass = ClassProvider.getClass(typeName);
			ReflectionUI reflectionUI = ReflectionUI.getDefault();
			JavaTypeInfoSource javaTypeInfoSource;
			if (typeOwner != null) {
				if (typeOwner instanceof GetterFieldInfo) {
					Method javaTypeOwner = ((GetterFieldInfo) typeOwner).getJavaGetterMethod();
					javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
				} else if (typeOwner instanceof PublicFieldInfo) {
					Field javaTypeOwner = ((PublicFieldInfo) typeOwner).getJavaField();
					javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
				} else if (typeOwner instanceof DefaultMethodInfo) {
					Method javaTypeOwner = ((DefaultMethodInfo) typeOwner).getJavaMethod();
					javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
				} else {
					throw new AssertionError();
				}
			} else {
				javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, null);
			}
			return reflectionUI.buildTypeInfo(javaTypeInfoSource);
		}

	}

	public static class ClassProvider {

		private static Set<ClassLoader> additionalClassLoaders = Collections
				.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

		public static Class<?> getClass(String typeName) {
			try {
				return Class.forName(typeName);
			} catch (ClassNotFoundException e) {
				for (ClassLoader classLoader : additionalClassLoaders) {
					try {
						return Class.forName(typeName, false, classLoader);
					} catch (ClassNotFoundException ignore) {
					}
				}
			}
			throw new AssertionError(new ClassNotFoundException(typeName));
		}

		public static void register(ClassLoader classLoader) {
			additionalClassLoaders.add(classLoader);
		}

		public static void unregister(ClassLoader classLoader) {
			additionalClassLoaders.remove(classLoader);
		}
	}

}
