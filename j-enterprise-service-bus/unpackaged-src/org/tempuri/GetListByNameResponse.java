
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
 *         &lt;element name="GetListByNameResult" type="{http://tempuri.org}ArrayOfPersonIdentificationPersonIdentification"/&gt;
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
    "getListByNameResult"
})
@XmlRootElement(name = "GetListByNameResponse")
public class GetListByNameResponse {

    @XmlElement(name = "GetListByNameResult", required = true)
    protected ArrayOfPersonIdentificationPersonIdentification getListByNameResult;

    /**
     * Gets the value of the getListByNameResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfPersonIdentificationPersonIdentification }
     *     
     */
    public ArrayOfPersonIdentificationPersonIdentification getGetListByNameResult() {
        return getListByNameResult;
    }

    /**
     * Sets the value of the getListByNameResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfPersonIdentificationPersonIdentification }
     *     
     */
    public void setGetListByNameResult(ArrayOfPersonIdentificationPersonIdentification value) {
        this.getListByNameResult = value;
    }

}
