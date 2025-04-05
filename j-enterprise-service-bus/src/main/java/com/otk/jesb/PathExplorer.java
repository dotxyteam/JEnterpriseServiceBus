package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

	public TypeNode getRootNode() {
		return new TypeNode(null, typeName);
	}

	public String getRootExpression() {
		return rootExpression;
	}

	public static interface PathNode {

		List<PathNode> getChildren();

		String getExpression();

	}

	public class TypeNode implements PathNode {

		protected PathNode parent;
		protected String typeName;

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

		@Override
		public String getExpression() {
			String parentExpression = (parent != null) ? parent.getExpression() : PathExplorer.this.getRootExpression();
			return parentExpression;
		}

		@Override
		public String toString() {
			return "<" + typeName + ">";
		}

	}

	public class FieldNode implements PathNode {

		protected TypeNode parent;
		protected String fieldName;

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
			return new TypeNode(this, getFieldInfo().getType().getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parent != null) ? parent.getExpression() : PathExplorer.this.getRootExpression();
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
		public String toString() {
			return fieldName;
		}
	}

	public class ListItemNode implements PathNode {

		protected TypeNode parent;

		public ListItemNode(TypeNode parent) {
			this.parent = parent;
		}

		public TypeNode getParent() {
			return parent;
		}

		public ITypeInfo getItemType() {
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parent.getTypeInfo();
			return parentTypeInfo.getItemType();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypeNode(this, getItemType().getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parent != null) ? parent.getExpression() : PathExplorer.this.getRootExpression();
			IListTypeInfo parentTypeInfo = (IListTypeInfo) parent.getTypeInfo();
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
		public String toString() {
			return "[i]";
		}
	}

	public class MapValueNode implements PathNode {

		protected TypeNode parent;

		public MapValueNode(TypeNode parent) {
			this.parent = parent;
		}

		public TypeNode getParent() {
			return parent;
		}

		public ITypeInfo getValueType() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			IMapEntryTypeInfo mapTypeInfo = (IMapEntryTypeInfo) ((IListTypeInfo) parentTypeInfo).getItemType();
			return mapTypeInfo.getValueField().getType();
		}

		@Override
		public List<PathNode> getChildren() {
			return new TypeNode(this, getValueType().getName()).getChildren();
		}

		@Override
		public String getExpression() {
			String parentExpression = (parent != null) ? parent.getExpression() : PathExplorer.this.getRootExpression();
			return parentExpression + ".get(*)";
		}

		@Override
		public String toString() {
			return "{*}";
		}
	}

}
