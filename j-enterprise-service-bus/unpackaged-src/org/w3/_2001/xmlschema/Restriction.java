
package org.w3._2001.xmlschema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * 
 *           base attribute and simpleType child are mutually
 *           exclusive, but one or other is required
 *         
 * 
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated"&gt;
 *       &lt;group ref="{http://www.w3.org/2001/XMLSchema}simpleRestrictionModel"/&gt;
 *       &lt;attribute name="base" type="{http://www.w3.org/2001/XMLSchema}QName" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "simpleType",
    "facets"
})
@XmlRootElement(name = "restriction")
public class Restriction
    extends Annotated
{

    protected LocalSimpleType simpleType;
    @XmlElementRefs({
        @XmlElementRef(name = "minExclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "minInclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxExclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxInclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "totalDigits", namespace = "http://www.w3.org/2001/XMLSchema", type = TotalDigits.class, required = false),
        @XmlElementRef(name = "fractionDigits", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "length", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "minLength", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxLength", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "enumeration", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "whiteSpace", namespace = "http://www.w3.org/2001/XMLSchema", type = WhiteSpace.class, required = false),
        @XmlElementRef(name = "pattern", namespace = "http://www.w3.org/2001/XMLSchema", type = Pattern.class, required = false)
    })
    protected List<Object> facets;
    @XmlAttribute(name = "base")
    protected QName base;

    /**
     * Gets the value of the simpleType property.
     * 
     * @return
     *     possible object is
     *     {@link LocalSimpleType }
     *     
     */
    public LocalSimpleType getSimpleType() {
        return simpleType;
    }

    /**
     * Sets the value of the simpleType property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocalSimpleType }
     *     
     */
    public void setSimpleType(LocalSimpleType value) {
        this.simpleType = value;
    }

    /**
     * Gets the value of the facets property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the facets property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFacets().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link TotalDigits }
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NoFixedFacet }{@code >}
     * {@link WhiteSpace }
     * {@link Pattern }
     * 
     * 
     */
    public List<Object> getFacets() {
        if (facets == null) {
            facets = new ArrayList<Object>();
        }
        return this.facets;
    }

    /**
     * Gets the value of the base property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getBase() {
        return base;
    }

    /**
     * Sets the value of the base property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setBase(QName value) {
        this.base = value;
    }

}
