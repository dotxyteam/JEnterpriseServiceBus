
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
 *         &lt;element name="GetByNameResult" type="{http://tempuri.org}DataSet"/&gt;
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
    "getByNameResult"
})
@XmlRootElement(name = "GetByNameResponse")
public class GetByNameResponse {

    @XmlElement(name = "GetByNameResult", required = true)
    protected DataSet getByNameResult;

    /**
     * Gets the value of the getByNameResult property.
     * 
     * @return
     *     possible object is
     *     {@link DataSet }
     *     
     */
    public DataSet getGetByNameResult() {
        return getByNameResult;
    }

    /**
     * Sets the value of the getByNameResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataSet }
     *     
     */
    public void setGetByNameResult(DataSet value) {
        this.getByNameResult = value;
    }

}
