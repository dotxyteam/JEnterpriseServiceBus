
package org.w3._2001.xmlschema;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;


/**
 * 
 *    group type for explicit groups, named top-level groups and
 *    group references
 * 
 * <p>Java class for group complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="group"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated"&gt;
 *       &lt;group ref="{http://www.w3.org/2001/XMLSchema}particle" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;attGroup ref="{http://www.w3.org/2001/XMLSchema}defRef"/&gt;
 *       &lt;attGroup ref="{http://www.w3.org/2001/XMLSchema}occurs"/&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group", propOrder = {
    "particle"
})
@XmlSeeAlso({
    RealGroup.class,
    ExplicitGroup.class
})
public abstract class Group
    extends Annotated
{

    @XmlElementRefs({
        @XmlElementRef(name = "element", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "group", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "all", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "choice", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "sequence", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "any", namespace = "http://www.w3.org/2001/XMLSchema", type = Any.class, required = false)
    })
    protected List<Object> particle;
    @XmlAttribute(name = "name")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String name;
    @XmlAttribute(name = "ref")
    protected QName ref;
    @XmlAttribute(name = "minOccurs")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger minOccurs;
    @XmlAttribute(name = "maxOccurs")
    @XmlSchemaType(name = "allNNI")
    protected String maxOccurs;

    /**
     * Gets the value of the particle property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the particle property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParticle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link LocalElement }{@code >}
     * {@link JAXBElement }{@code <}{@link GroupRef }{@code >}
     * {@link JAXBElement }{@code <}{@link All }{@code >}
     * {@link JAXBElement }{@code <}{@link ExplicitGroup }{@code >}
     * {@link JAXBElement }{@code <}{@link ExplicitGroup }{@code >}
     * {@link Any }
     * 
     * 
     */
    public List<Object> getParticle() {
        if (particle == null) {
            particle = new ArrayList<Object>();
        }
        return this.particle;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the ref property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getRef() {
        return ref;
    }

    /**
     * Sets the value of the ref property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setRef(QName value) {
        this.ref = value;
    }

    /**
     * Gets the value of the minOccurs property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMinOccurs() {
        if (minOccurs == null) {
            return new BigInteger("1");
        } else {
            return minOccurs;
        }
    }

    /**
     * Sets the value of the minOccurs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMinOccurs(BigInteger value) {
        this.minOccurs = value;
    }

    /**
     * Gets the value of the maxOccurs property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaxOccurs() {
        if (maxOccurs == null) {
            return "1";
        } else {
            return maxOccurs;
        }
    }

    /**
     * Sets the value of the maxOccurs property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaxOccurs(String value) {
        this.maxOccurs = value;
    }

}
