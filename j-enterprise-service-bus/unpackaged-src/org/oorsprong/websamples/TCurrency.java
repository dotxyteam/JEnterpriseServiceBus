
package org.oorsprong.websamples;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tCurrency complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tCurrency"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="sISOCode" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="sName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCurrency", propOrder = {
    "sisoCode",
    "sName"
})
public class TCurrency {

    @XmlElement(name = "sISOCode", required = true)
    protected String sisoCode;
    @XmlElement(required = true)
    protected String sName;

    /**
     * Gets the value of the sisoCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSISOCode() {
        return sisoCode;
    }

    /**
     * Sets the value of the sisoCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSISOCode(String value) {
        this.sisoCode = value;
    }

    /**
     * Gets the value of the sName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSName() {
        return sName;
    }

    /**
     * Sets the value of the sName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSName(String value) {
        this.sName = value;
    }

}
