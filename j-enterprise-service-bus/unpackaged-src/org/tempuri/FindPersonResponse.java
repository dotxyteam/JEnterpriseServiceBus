
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
 *         &lt;element name="FindPersonResult" type="{http://tempuri.org}Person"/&gt;
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
    "findPersonResult"
})
@XmlRootElement(name = "FindPersonResponse")
public class FindPersonResponse {

    @XmlElement(name = "FindPersonResult", required = true)
    protected Person findPersonResult;

    /**
     * Gets the value of the findPersonResult property.
     * 
     * @return
     *     possible object is
     *     {@link Person }
     *     
     */
    public Person getFindPersonResult() {
        return findPersonResult;
    }

    /**
     * Sets the value of the findPersonResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link Person }
     *     
     */
    public void setFindPersonResult(Person value) {
        this.findPersonResult = value;
    }

}
