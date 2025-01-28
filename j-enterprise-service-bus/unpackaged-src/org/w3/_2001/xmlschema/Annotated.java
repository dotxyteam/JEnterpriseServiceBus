
package org.w3._2001.xmlschema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *        This type is extended by all types which allow annotation
 *        other than <schema> itself
 *      
 * 
 * <p>Java class for annotated complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="annotated"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}openAttrs"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://www.w3.org/2001/XMLSchema}annotation" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "annotated", propOrder = {
    "annotation"
})
@XmlSeeAlso({
    ExtensionType.class,
    Attribute.class,
    ComplexType.class,
    RestrictionType.class,
    Group.class,
    Element.class,
    AttributeGroup.class,
    SimpleType.class,
    Union.class,
    List.class,
    Facet.class,
    Restriction.class,
    Keybase.class,
    Field.class,
    Selector.class,
    Wildcard.class,
    SimpleContent.class,
    ComplexContent.class,
    Notation.class,
    Import.class,
    Include.class
})
public class Annotated
    extends OpenAttrs
{

    protected Annotation annotation;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    /**
     * Gets the value of the annotation property.
     * 
     * @return
     *     possible object is
     *     {@link Annotation }
     *     
     */
    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Sets the value of the annotation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Annotation }
     *     
     */
    public void setAnnotation(Annotation value) {
        this.annotation = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
