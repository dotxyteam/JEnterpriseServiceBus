
package org.tempuri;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="LookupCityResult" type="{http://tempuri.org}Address"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "lookupCityResult"
})
@XmlRootElement(name = "LookupCityResponse")
public class LookupCityResponse {

    @XmlElement(name = "LookupCityResult", required = true)
    protected Address lookupCityResult;

    /**
     * Gets the value of the lookupCityResult property.
     * 
     * @return
     *     possible object is
     *     {@link Address }
     *     
     */
    public Address getLookupCityResult() {
        return lookupCityResult;
    }

    /**
     * Sets the value of the lookupCityResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link Address }
     *     
     */
    public void setLookupCityResult(Address value) {
        this.lookupCityResult = value;
    }

}
