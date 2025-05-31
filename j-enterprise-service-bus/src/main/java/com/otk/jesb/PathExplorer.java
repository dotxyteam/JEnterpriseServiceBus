package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.otk.jesb.meta.TypeInfoProvider;
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
					result.add(new StreamNode(this));
				}
			} else if (Stream.class.isAssignableFrom(((DefaultTypeInfo) typeInfo).getJavaType())) {
				result.add(new StreamFilteringNode(this));
				result.add(new StreamMappingNode(this));
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
						ListSizeNode.class, StreamNode.class);
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
						throw new UnexpectedError();
					}
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
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, int.class.getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return parentExpression + ".length";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".size())";
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
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return Arrays.class.getName() + ".asList(" + parentExpression + ").stream()";
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
				return "\\s*" + Arrays.class.getName().replace(".", "\\.") + "\\s*\\.asList\\s*\\(\\s*" + parentPattern
						+ "\\s*\\)\\s*\\.\\s*stream\\s*\\(\\s*\\)\\s*";
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
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(itemType != null) ? new Class<?>[] { ((DefaultTypeInfo) itemType).getJavaType() } : null);
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
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, Stream.class.getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
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
			Class<?>[] parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.getGenericTypeParameters();
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters[0] } : null);
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
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getExpressionType().getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
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
			Class<?>[] parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.getGenericTypeParameters();
			return TypeInfoProvider.getTypeInfo(Stream.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters[0] } : null);
		}

		@Override
		public String toString() {
			return "<map>";
		}
	}

	public class StreamListCollectorNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public StreamListCollectorNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
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
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
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
			Class<?>[] parentStreamTypeParameters = ((JavaTypeInfoSource) parentTypeInfo.getSource())
					.getGenericTypeParameters();
			return TypeInfoProvider.getTypeInfo(List.class,
					(parentStreamTypeParameters != null) ? new Class<?>[] { parentStreamTypeParameters[0] } : null);
		}

		@Override
		public String toString() {
			return "<collect>";
		}
	}

	public class ListStringJoiningNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public ListStringJoiningNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, String.class.getName()).getChildren();
		}

		@Override
		public String getTypicalExpression() {
			Arrays.asList().stream().map(Objects::toString).collect(Collectors.joining());
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getTypicalExpression()
					: PathExplorer.this.getTypicalRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return Arrays.class.getName() + ".asList(" + parentExpression + ").stream().map("
						+ Objects.class.getName() + "::toString).collect(" + Collectors.class.getName() + ".joining())";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".stream().map(" + Objects.class.getName() + "::toString).collect("
						+ Collectors.class.getName() + ".joining())";
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
				return "\\s*" + Arrays.class.getName().replace(".", "\\.") + "\\s*\\.asList\\s*\\(\\s*" + parentPattern
						+ "\\s*\\)\\s*\\.\\s*stream\\s*\\(\\s*\\)\\s*\\.\\s*map\\s*\\(\\s*"
						+ Objects.class.getName().replace(".", "\\.")
						+ "\\s*::\\s*toString\\s*\\)\\s*\\.collect\\s*\\(\\s*"
						+ Collectors.class.getName().replace(".", "\\.")
						+ "\\s*\\.\\s*joining\\s*\\(\\s*\\)\\s*\\)\\s*";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return "\\s*" + parentPattern + "\\s*\\.\\s*stream\\s*\\(\\s*\\)\\s*\\.\\s*map\\s*\\(\\s*"
						+ Objects.class.getName().replace(".", "\\.")
						+ "\\s*::\\s*toString\\s*\\)\\s*\\.collect\\s*\\(\\s*"
						+ Collectors.class.getName().replace(".", "\\.")
						+ "\\s*\\.\\s*joining\\s*\\(\\s*\\)\\s*\\)\\s*";
			} else {
				throw new UnexpectedError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			return TypeInfoProvider.getTypeInfo(String.class);
		}

		@Override
		public String toString() {
			return "<stringJoin>";
		}
	}

	public class MapValueNode implements PathNode {

		protected TypedNodeUtility parentTypedNodeUtility;

		public MapValueNode(TypedNodeUtility parent) {
			this.parentTypedNodeUtility = parent;
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
