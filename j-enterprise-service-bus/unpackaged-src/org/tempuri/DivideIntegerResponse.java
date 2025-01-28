
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
 *         &lt;element name="DivideIntegerResult" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
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
    "divideIntegerResult"
})
@XmlRootElement(name = "DivideIntegerResponse")
public class DivideIntegerResponse {

    @XmlElement(name = "DivideIntegerResult")
    protected long divideIntegerResult;

    /**
     * Gets the value of the divideIntegerResult property.
     * 
     */
    public long getDivideIntegerResult() {
        return divideIntegerResult;
    }

    /**
     * Sets the value of the divideIntegerResult property.
     * 
     */
    public void setDivideIntegerResult(long value) {
        this.divideIntegerResult = value;
    }

}
