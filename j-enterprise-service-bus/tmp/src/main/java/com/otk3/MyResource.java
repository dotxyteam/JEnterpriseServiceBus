package com.otk3;
public class MyResource extends com.otk.jesb.resource.Resource{
private java.lang.String prop1;
private com.otk.jesb.Variant<java.lang.Integer> prop2Variant=new com.otk.jesb.Variant<java.lang.Integer>(java.lang.Integer.class, -1);
private Prop3Structure prop3=new Prop3Structure();
public MyResource(){

}
public java.lang.String getProp1() {
return prop1;
}
public void setProp1(java.lang.String prop1) {
this.prop1 = prop1;
}

public com.otk.jesb.Variant<java.lang.Integer> getProp2Variant() {
return prop2Variant;
}
public void setProp2Variant(com.otk.jesb.Variant<java.lang.Integer> prop2Variant) {
this.prop2Variant = prop2Variant;
}

public Prop3Structure getProp3() {
return prop3;
}
public void setProp3(Prop3Structure prop3) {
this.prop3 = prop3;
}

@Override
public String toString() {
return "MyResource [prop1=" + prop1 + ", prop2Variant=" + prop2Variant + ", prop3=" + prop3 + "]";
}
@Override
public void validate(boolean recursively) {
}
public static class Metadata implements com.otk.jesb.resource.ResourceMetadata{
@Override
public String getResourceTypeName() {
return "My Resource";
}
@Override
public Class<? extends com.otk.jesb.resource.Resource> getResourceClass() {
return MyResource.class;
}
@Override
public xy.reflect.ui.info.ResourcePath getResourceIconImagePath() {
return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath.specifyClassPathResourceLocation(MyResource.class.getName().replace(".", "/") + ".png"));
}
}

static package null;
public class Prop3Structure impelments com.otk.jesb.resource.ResourceStructure{
private java.lang.String subProp1;
private java.lang.Integer subProp2=10;
public Prop3Structure(){

}
public java.lang.String getSubProp1() {
return subProp1;
}
public void setSubProp1(java.lang.String subProp1) {
this.subProp1 = subProp1;
}

public java.lang.Integer getSubProp2() {
return subProp2;
}
public void setSubProp2(java.lang.Integer subProp2) {
this.subProp2 = subProp2;
}

@Override
public String toString() {
return "Prop3Structure [subProp1=" + subProp1 + ", subProp2=" + subProp2 + "]";
}
@Override
public void validate(boolean recursively) {
}



}
}