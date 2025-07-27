package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.ArrayStream;
import com.otk.jesb.util.InstantiationUtils;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.ArrayTypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.StandardCollectionTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PathExplorer {

	private String typeName;
	private String rootVariableName;

	public PathExplorer(String typeName, String rootVariableName) {
		this.typeName = typeName;
		this.rootVariableName = rootVariableName;
	}

	public List<PathNode> explore() {
		return new TypedNodeUtility(null, typeName).getChildren();
	}

	public String getTypicalRootExpression() {
		return rootVariableName;
	}

	public String getRootExpressionPattern() {
		return "\\s*\\b" + rootVariableName + "\\b\\s*";
	}

	public ITypeInfo getRootExpressionType() {
		return TypeInfoProvider.getTypeInfo(typeName);
	}

	public static interface PathNode {

		PathNode getParent();

		List<PathNode> getChildren();

		String getTypicalExpression();

		String getExpressionPattern();

		ITypeInfo getExpressionType();

		PathExplorer getExplorer();

	}

	public static class PathNodeProxy implements PathNode {
		private PathNode base;

		public PathNodeProxy(PathNode base) {
			this.base = base;
		}

		@Override
		public PathExplorer getExplorer() {
			return base.getExplorer();
		}

		@Override
		public PathNode getParent() {
			return base.getParent();
		}

		@Override
		public List<PathNode> getChildren() {
			return base.getChildren();
		}

		@Override
		public String getTypicalExpression() {
			return base.getTypicalExpression();
		}

		@Override
		public String getExpressionPattern() {
			return base.getExpressionPattern();
		}

		@Override
		public ITypeInfo getExpressionType() {
			return base.getExpressionType();
		}

	}

	private class TypedNodeUtility {

		protected PathNode node;
		protected String typeName;

		public TypedNodeUtility(PathNode node, String typeName) {
			this.node = node;
			this.typeName = typeName;
		}

		public PathNode getNode() {
			return node;
		}

		public ITypeInfo getTypeInfo() {
			return (node != null) ? node.getExpressionType() : PathExplorer.this.getRootExpressionType();
		}

		public List<PathNode> getChildren() {
			List<PathNode> result = new ArrayList<PathNode>();
			ITypeInfo typeInfo = getTypeInfo();
			if (typeInfo instanceof IListTypeInfo) {
				if (((IListTypeInfo) typeInfo).getItemType() instanceof IMapEntryTypeInfo) {
					result.add(new MapValueNode(this));
				} else {
					result.add(new ListItemNode(this));
					result.add(new ListSizeNode(this));
					result.add(new ListFilteringNode(this));
					result.add(new ListMappingNode(this));
					result.add(new ListReducingNode(this));
				}
			} else if (Stream.class.isAssignableFrom(((DefaultTypeInfo) typeInfo).getJavaType())) {
				result.add(new StreamFilteringNode(this));
				result.add(new StreamMappingNode(this));
				result.add(new StreamReducingNode(this));
				result.add(new StreamListCollectorNode(this));
			} else {
				if (InstantiationUtils.isComplexType(typeInfo)) {
					if (!typeInfo.getName().equals(Object.class.getName())) {
						for (IFieldInfo field : typeInfo.getFields()) {
							result.add(new FieldNode(this, field.getName()));
						}
					}
				}
			}
			Collections.sort(result, new Comparator<PathNode>() {
				List<Class<?>> CLASSES_ORDER = Arrays.asList(FieldNode.class, ListItemNode.class, MapValueNode.class,
						ListSizeNode.class, StreamNode.class, StreamFilteringNode.class, StreamMappingNode.class,
						StreamListCollectorNode.class);

				@Override
				public int compare(PathNode o1, PathNode o2) {
					if (!o1.getClass().equals(o2.getClass())) {
						Class<?> class1 = o1.getClass();
						{
							while (class1.isAnonymousClass()) {
								class1 = class1.getSuperclass();
							}
						}
						Class<?> class2 = o1.getClass();
						{
							while (class2.isAnonymousClass()) {
								class2 = class2.getSuperclass();
							}
						}
						return Integer.valueOf(CLASSES_ORDER.indexOf(class1))
								.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(class2)));
					}
					if ((o1 instanceof FieldNode) && (o2 instanceof FieldNode)) {
						FieldNode fn1 = (FieldNode) o1;
						FieldNode fn2 = (FieldNode) o2;
						return fn1.getFieldName().compareTo(fn2.getFieldName());
					}
					return o1.toString().compareTo(o2.toString());
				}
			});
			return result;
		}

		public String getTypicalExpression() {
			String parentExpression = (node != null) ? node.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			return parentExpression;
		}

		public String getExpressionPattern() {
			String parentPattern = (node != null) ? node.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return parentPattern;
		}

		@Override
		public String toString() {
			return "<" + typeName + ">";
		}

	}

	public class FieldNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;
		protected String fieldName;

		public FieldNode(TypedNodeUtility parentTypedNodeUtility, String fieldName) {
			this.parentTypedNodeUtility = parentTypedNodeUtility;
			this.fieldName = fieldName;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		public String getFieldName() {
			return fieldName;
		}

		public IFieldInfo getFieldInfo() {
			return ReflectionUIUtils.findInfoByName(parentTypedNodeUtility.getTypeInfo().getFields(), fieldName);
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IFieldInfo fieldInfo = getFieldInfo();
			if (fieldInfo instanceof GetterFieldInfo) {
				return parentExpression + "." + ((GetterFieldInfo) fieldInfo).getJavaGetterMethod().getName() + "()";
			} else if (fieldInfo instanceof PublicFieldInfo) {
				return parentExpression + "." + ((PublicFieldInfo) fieldInfo).getJavaField().getName();
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			IFieldInfo fieldInfo = getFieldInfo();
			if (fieldInfo instanceof GetterFieldInfo) {
				return parentPattern + "\\s*\\.\\s*" + ((GetterFieldInfo) fieldInfo).getJavaGetterMethod().getName()
						+ "\\s*\\(\\s*\\)\\s*";
			} else if (fieldInfo instanceof PublicFieldInfo) {
				return parentPattern + "\\s*\\.\\s*\\b" + ((PublicFieldInfo) fieldInfo).getJavaField().getName()
						+ "\\b\\s*";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			IFieldInfo fieldInfo = getFieldInfo();
			ITypeInfo result = fieldInfo.getType();
			if (result instanceof IListTypeInfo) {
				result = TypeInfoProvider.getTypeInfo(result.getName(), fieldInfo);
			}
			return result;
		}

		@Override
		public String toString() {
			return fieldName;
		}
	}

	public class ListItemNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public ListItemNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return parentExpression + "[?]";
			} else if (List.class.isAssignableFrom(((DefaultTypeInfo) parentTypeInfo).getJavaType())) {
				return parentExpression + ".get(?)";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".iterator().next()";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return parentPattern + "\\s*\\[\\s*[^\\[\\]]+\\s*\\]\\s*";
			} else if (List.class.isAssignableFrom(((DefaultTypeInfo) parentTypeInfo).getJavaType())) {
				return parentPattern + "\\s*\\.\\s*get\\s*\\(\\s*[^\\(\\)]+\\s*\\)\\s*";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentPattern + "\\s*\\.\\s*iterator\\s*\\(\\s*\\)\\s*\\.\\s*next\\s*\\(\\s*\\)\\s*";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			ITypeInfo result = parentTypeInfo.getItemType();
			if (result == null) {
				result = TypeInfoProvider.getTypeInfo(Object.class);
			}
			return result;

		}

		@Override
		public String toString() {
			return "[i]";
		}
	}

	public class ListSizeNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public ListSizeNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, int.class.getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return parentExpression + ".length";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".size()";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return "\\s*" + parentPattern + "\\s*\\.\\s*length\\s*";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return "\\s*" + parentPattern + "\\s*\\.\\s*size\\s*\\(\\s*\\)\\s*";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			return TypeInfoProvider.getTypeInfo(int.class);
		}

		@Override
		public String toString() {
			return "<count>";
		}
	}

	public class StreamNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return ArrayStream.class.getName() + ".get(" + parentExpression + ")";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".stream()";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return "\\s*" + ArrayStream.class.getName().replace(".", "\\.") + "\\s*\\.get\\s*\\(\\s*"
						+ parentPattern + "\\s*\\)\\s*\\.\\s*";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return "\\s*" + parentPattern + "\\s*\\.\\s*stream\\s*\\(\\s*\\)\\s*";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			ITypeInfo itemType = parentTypeInfo.getItemType();
			Class<?> itemClass;
			if (itemType != null) {
				itemClass = ((DefaultTypeInfo) itemType).getJavaType();
				if (itemType.isPrimitive()) {
					itemClass = ClassUtils.primitiveToWrapperClass(itemClass);
				}
			} else {
				itemClass = null;
			}
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(itemClass != null) ? new Class<?>[] { itemClass } : null);
		}

		@Override
		public String toString() {
			return "<stream>";
		}
	}

	public class StreamFilteringNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamFilteringNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, Stream.class.getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			if (!Stream.class
					.isAssignableFrom(((DefaultTypeInfo) parentTypedNodeUtility.getTypeInfo()).getJavaType())) {
				throw new UnexpectedError();
			}
			return parentExpression + ".filter(element -> ?)";
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return "\\s*" + parentPattern + "\\s*\\.\\s*filter\\s*\\([^\\(\\)]+\\)\\s*";
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			List<Class<?>> parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.guessGenericTypeParameters(Stream.class);
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters.get(0) } : null);
		}

		@Override
		public String toString() {
			return "<filter>";
		}
	}

	public class StreamMappingNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamMappingNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			if (!Stream.class
					.isAssignableFrom(((DefaultTypeInfo) parentTypedNodeUtility.getTypeInfo()).getJavaType())) {
				throw new UnexpectedError();
			}
			return parentExpression + ".map(element -> ?)";
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return "\\s*" + parentPattern + "\\s*\\.\\s*map\\s*\\([^\\(\\)]+\\)\\s*";
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			List<Class<?>> parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.guessGenericTypeParameters(Stream.class);
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters.get(0) } : null);
		}

		@Override
		public String toString() {
			return "<map>";
		}
	}

	public class StreamReducingNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamReducingNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			if (!Stream.class
					.isAssignableFrom(((DefaultTypeInfo) parentTypedNodeUtility.getTypeInfo()).getJavaType())) {
				throw new UnexpectedError();
			}
			return parentExpression + ".reduce((element1, element2) -> ?).orElse(?)";
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return "\\s*" + parentPattern
					+ "\\s*\\.\\s*reduce\\s*\\([^\\(\\)]+\\)\\s*\\.\\s*orElse\\s*\\([^\\(\\)]+\\)\\s*";
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			List<Class<?>> parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.guessGenericTypeParameters(Stream.class);
			return TypeInfoProvider.getTypeInfo(
					(parentStreamTypeParameters != null) ? parentStreamTypeParameters.get(0) : Object.class);
		}

		@Override
		public String toString() {
			return "<reduce>";
		}
	}

	public class StreamListCollectorNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamListCollectorNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			if (!Stream.class
					.isAssignableFrom(((DefaultTypeInfo) parentTypedNodeUtility.getTypeInfo()).getJavaType())) {
				throw new UnexpectedError();
			}
			return parentExpression + ".collect(" + Collectors.class.getName() + ".toList())";
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return "\\s*" + parentPattern + "\\s*\\.\\s*collect\\s*\\(\\s*"
					+ Collectors.class.getName().replace(".", "\\.") + "\\s*toList\\s*\\(\\s*\\)\\s*\\)\\s*";
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			List<Class<?>> parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.guessGenericTypeParameters(Stream.class);
			return TypeInfoProvider.getTypeInfo(List.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters.get(0) } : null);
		}

		@Override
		public String toString() {
			return "<collect>";
		}
	}

	public class ListFilteringNode extends PathNodeProxy {

		public ListFilteringNode(TypedNodeUtility parentTypedNodeUtility) {
			super(((parentTypedNodeUtility.getNode() instanceof StreamListCollectorNode)
					? parentTypedNodeUtility.getNode().getParent()
					: new StreamNode(parentTypedNodeUtility)).getChildren().stream()
							.filter(StreamFilteringNode.class::isInstance).findFirst().get().getChildren().stream()
							.filter(StreamListCollectorNode.class::isInstance).findFirst().get());
		}

		@Override
		public String toString() {
			return "<filtered>";
		}
	}

	public class ListMappingNode extends PathNodeProxy {

		public ListMappingNode(TypedNodeUtility parentTypedNodeUtility) {
			super(((parentTypedNodeUtility.getNode() instanceof StreamListCollectorNode)
					? parentTypedNodeUtility.getNode().getParent()
					: new StreamNode(parentTypedNodeUtility)).getChildren().stream()
							.filter(StreamMappingNode.class::isInstance).findFirst().get().getChildren().stream()
							.filter(StreamListCollectorNode.class::isInstance).findFirst().get());
		}

		@Override
		public String toString() {
			return "<mapped>";
		}
	}

	public class ListReducingNode extends PathNodeProxy {

		public ListReducingNode(TypedNodeUtility parentTypedNodeUtility) {
			super(((parentTypedNodeUtility.getNode() instanceof StreamListCollectorNode)
					? parentTypedNodeUtility.getNode().getParent()
					: new StreamNode(parentTypedNodeUtility)).getChildren().stream()
							.filter(StreamReducingNode.class::isInstance).findFirst().get());
		}

		@Override
		public String toString() {
			return "<reduced>";
		}
	}

	public class MapValueNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public MapValueNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathExplorer getExplorer() {
			return PathExplorer.this;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			return parentExpression + ".get(*)";
		}

		@Override
		public String getExpressionPattern() {
			String parentPattern = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpressionPattern()
					: PathExplorer.this.getRootExpressionPattern();
			return parentPattern + "\\s*\\.\\s*get\\s*\\(\\s*[^\\(\\)]+\\s*\\)\\s*";
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			IMapEntryTypeInfo mapTypeInfo = (IMapEntryTypeInfo) ((IListTypeInfo) parentTypeInfo).getItemType();
			return mapTypeInfo.getValueField().getType();
		}

		@Override
		public String toString() {
			return "{*}";
		}
	}

	public static class RelativePathNode implements PathNode {

		private PathNode underlying;
		private String typicalReferenceExpression;
		private String referenceExpressionPattern;
		private String referenceVariableName;

		public RelativePathNode(PathNode underlying, String typicalReferenceExpression,
				String referenceExpressionPattern, String referenceVariableName) {
			this.underlying = underlying;
			this.typicalReferenceExpression = typicalReferenceExpression;
			this.referenceExpressionPattern = referenceExpressionPattern;
			this.referenceVariableName = referenceVariableName;
		}

		public PathNode getUnderlying() {
			return underlying;
		}

		public String getTypicalReferenceExpression() {
			return typicalReferenceExpression;
		}

		public String getReferenceExpressionPattern() {
			return referenceExpressionPattern;
		}

		public String getReferenceVariableName() {
			return referenceVariableName;
		}

		@Override
		public PathExplorer getExplorer() {
			return underlying.getExplorer();
		}

		@Override
		public PathNode getParent() {
			PathNode result = underlying.getParent();
			if (result != null) {
				result = new RelativePathNode(result, typicalReferenceExpression, referenceExpressionPattern,
						referenceVariableName);
			}
			return result;
		}

		@Override
		public List<PathNode> getChildren() {
			return underlying.getChildren().stream().map(child -> new RelativePathNode(child,
					typicalReferenceExpression, referenceExpressionPattern, referenceVariableName))
					.collect(Collectors.toList());
		}

		@Override
		public String getTypicalExpression() {
			return underlying.getTypicalExpression().replace(typicalReferenceExpression, referenceVariableName);
		}

		@Override
		public String getExpressionPattern() {
			return underlying.getExpressionPattern().replace(referenceExpressionPattern,
					"\\s*\\b" + referenceVariableName + "\\b\\s*");
		}

		@Override
		public ITypeInfo getExpressionType() {
			return underlying.getExpressionType();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((referenceExpressionPattern == null) ? 0 : referenceExpressionPattern.hashCode());
			result = prime * result + ((referenceVariableName == null) ? 0 : referenceVariableName.hashCode());
			result = prime * result
					+ ((typicalReferenceExpression == null) ? 0 : typicalReferenceExpression.hashCode());
			result = prime * result + ((underlying == null) ? 0 : underlying.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RelativePathNode other = (RelativePathNode) obj;
			if (referenceExpressionPattern == null) {
				if (other.referenceExpressionPattern != null)
					return false;
			} else if (!referenceExpressionPattern.equals(other.referenceExpressionPattern))
				return false;
			if (referenceVariableName == null) {
				if (other.referenceVariableName != null)
					return false;
			} else if (!referenceVariableName.equals(other.referenceVariableName))
				return false;
			if (typicalReferenceExpression == null) {
				if (other.typicalReferenceExpression != null)
					return false;
			} else if (!typicalReferenceExpression.equals(other.typicalReferenceExpression))
				return false;
			if (underlying == null) {
				if (other.underlying != null)
					return false;
			} else if (!underlying.equals(other.underlying))
				return false;
			return true;
		}

		public String toString() {
			return underlying.toString();
		}

	}

}
