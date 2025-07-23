
package https.soap_service_free_mock_beeceptor_com.countryinfoservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ListOfContinentsByNameResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ListOfContinentsByNameResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ListOfContinentsByNameResult" type="{https://soap-service-free.mock.beeceptor.com/CountryInfoService}ArrayOfContinents"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ListOfContinentsByNameResponseType", propOrder = {
    "listOfContinentsByNameResult"
})
public class ListOfContinentsByNameResponseType {

    @XmlElement(name = "ListOfContinentsByNameResult", required = true)
    protected ArrayOfContinents listOfContinentsByNameResult;

    /**
     * Gets the value of the listOfContinentsByNameResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfContinents }
     *     
     */
    public ArrayOfContinents getListOfContinentsByNameResult() {
        return listOfContinentsByNameResult;
    }

    /**
     * Sets the value of the listOfContinentsByNameResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfContinents }
     *     
     */
    public void setListOfContinentsByNameResult(ArrayOfContinents value) {
        this.listOfContinentsByNameResult = value;
    }

}
