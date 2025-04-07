package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.ArrayTypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.StandardCollectionTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntryTypeInfo;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PathExplorer {

	private String typeName;
	private String rootExpression;

	public PathExplorer(String typeName, String rootExpression) {
		this.typeName = typeName;
		this.rootExpression = rootExpression;
	}

	public List<PathNode> explore() {
		return new TypedNodeUtility(null, typeName).getChildren();
	}

	public String getRootExpression() {
		return rootExpression;
	}

	public ITypeInfo getRootExpressionType() {
		return new TypedNodeUtility(null, typeName).getTypeInfo();
	}

	public static interface PathNode {

		PathNode getParent();

		List<PathNode> getChildren();

		String getExpression();

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
			ITypeInfo result = TypeInfoProvider.getTypeInfo(typeName);
			if (result instanceof IListTypeInfo) {
				if (node instanceof FieldNode) {
					FieldNode fieldNode = (FieldNode) node;
					IFieldInfo listFieldInfo = fieldNode.getFieldInfo();
					result = TypeInfoProvider.getTypeInfo(typeName, listFieldInfo);
				}
			}
			if (result instanceof IMapEntryTypeInfo) {
				if (node instanceof ListItemNode) {
					ListItemNode mapEntryNode = (ListItemNode) node;
					result = (StandardMapEntryTypeInfo) mapEntryNode.getItemType();
				}
			}
			return result;
		}

		public List<PathNode> getChildren() {
			List<PathNode> result = new ArrayList<PathNode>();
			ITypeInfo typeInfo = getTypeInfo();
			if (typeInfo instanceof IListTypeInfo) {
				if (((IListTypeInfo) typeInfo).getItemType() instanceof IMapEntryTypeInfo) {
					result.add(new MapValueNode(this));
				} else {
					result.add(new ListItemNode(this));
				}
			} else if (!MiscUtils.isComplexType(typeInfo)) {
				return Collections.emptyList();
			} else {
				for (IFieldInfo field : typeInfo.getFields()) {
					result.add(new FieldNode(this, field.getName()));
				}
			}
			Collections.sort(result, new Comparator<PathNode>() {
				List<Class<?>> CLASSES_ORDER = Arrays.asList(FieldNode.class, ListItemNode.class, MapValueNode.class);

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

		public String getExpression() {
			String parentExpression = (node != null) ? node.getExpression() : PathExplorer.this.getRootExpression();
			return parentExpression;
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
			return new TypedNodeUtility(this, getFieldInfo().getType().getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpression()
					: PathExplorer.this.getRootExpression();
			IFieldInfo fieldInfo = getFieldInfo();
			if (fieldInfo instanceof GetterFieldInfo) {
				return parentExpression + "." + ((GetterFieldInfo) fieldInfo).getJavaGetterMethod().getName() + "()";
			} else if (fieldInfo instanceof PublicFieldInfo) {
				return parentExpression + "." + ((PublicFieldInfo) fieldInfo).getJavaField().getName();
			} else {
				throw new AssertionError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			return new TypedNodeUtility(this, getFieldInfo().getType().getName()).getTypeInfo();
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

		public ITypeInfo getItemType() {
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			return parentTypeInfo.getItemType();
		}

		@Override
		public PathNode getParent() {
			return parentTypedNodeUtility.getNode();
		}

		@Override
		public List<PathNode> getChildren() {
			ITypeInfo itemType = getItemType();
			if (itemType == null) {
				return Collections.emptyList();
			}
			return new TypedNodeUtility(this, itemType.getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpression()
					: PathExplorer.this.getRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parentTypedNodeUtility.getTypeInfo();
			if (parentTypeInfo instanceof ArrayTypeInfo) {
				return parentExpression + "[i]";
			} else if (List.class.isAssignableFrom(((DefaultTypeInfo) parentTypeInfo).getJavaType())) {
				return parentExpression + ".get(i)";
			} else if (parentTypeInfo instanceof StandardCollectionTypeInfo) {
				return parentExpression + ".iterator().next()";
			} else {
				throw new AssertionError();
			}
		}

		@Override
		public ITypeInfo getExpressionType() {
			ITypeInfo itemType = getItemType();
			String itemTypeName = (itemType != null) ? itemType.getName() : Object.class.getName();
			return new TypedNodeUtility(this, itemTypeName).getTypeInfo();
		}

		@Override
		public String toString() {
			return "[i]";
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

		public ITypeInfo getValueType() {
			ITypeInfo parentTypeInfo = parentTypedNodeUtility.getTypeInfo();
			IMapEntryTypeInfo mapTypeInfo = (IMapEntryTypeInfo) ((IListTypeInfo) parentTypeInfo).getItemType();
			return mapTypeInfo.getValueField().getType();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypedNodeUtility(this, getValueType().getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parentTypedNodeUtility != null) ? parentTypedNodeUtility.getExpression()
					: PathExplorer.this.getRootExpression();
			return parentExpression + ".get(*)";
		}

		@Override
		public ITypeInfo getExpressionType() {
			return new TypedNodeUtility(this, getValueType().getName()).getTypeInfo();
		}

		@Override
		public String toString() {
			return "{*}";
		}
	}

	public static class RelativePathNode implements PathNode {

		private PathNode underlying;
		private String referenceExpression;
		private String referenceVariableName;

		public RelativePathNode(PathNode underlying, String referenceExpression, String referenceVariableName) {
			this.underlying = underlying;
			this.referenceExpression = referenceExpression;
			this.referenceVariableName = referenceVariableName;
		}

		@Override
		public PathNode getParent() {
			PathNode result = underlying.getParent();
			if (result != null) {
				result = new RelativePathNode(result, referenceExpression, referenceVariableName);
			}
			return result;
		}

		@Override
		public List<PathNode> getChildren() {
			return underlying.getChildren().stream()
					.map(child -> new RelativePathNode(child, referenceExpression, referenceVariableName))
					.collect(Collectors.toList());
		}

		@Override
		public String getExpression() {
			return underlying.getExpression().replace(referenceExpression, referenceVariableName);
		}

		@Override
		public ITypeInfo getExpressionType() {
			return underlying.getExpressionType();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((referenceExpression == null) ? 0 : referenceExpression.hashCode());
			result = prime * result + ((referenceVariableName == null) ? 0 : referenceVariableName.hashCode());
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
			if (referenceExpression == null) {
				if (other.referenceExpression != null)
					return false;
			} else if (!referenceExpression.equals(other.referenceExpression))
				return false;
			if (referenceVariableName == null) {
				if (other.referenceVariableName != null)
					return false;
			} else if (!referenceVariableName.equals(other.referenceVariableName))
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
