
package https.soap_service_free_mock_beeceptor_com.countryinfoservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ListOfCountryNamesByNameResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ListOfCountryNamesByNameResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ListOfCountryNamesByNameResult" type="{https://soap-service-free.mock.beeceptor.com/CountryInfoService}ArrayOfCountryNames"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ListOfCountryNamesByNameResponseType", propOrder = {
    "listOfCountryNamesByNameResult"
})
public class ListOfCountryNamesByNameResponseType {

    @XmlElement(name = "ListOfCountryNamesByNameResult", required = true)
    protected ArrayOfCountryNames listOfCountryNamesByNameResult;

    /**
     * Gets the value of the listOfCountryNamesByNameResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfCountryNames }
     *     
     */
    public ArrayOfCountryNames getListOfCountryNamesByNameResult() {
        return listOfCountryNamesByNameResult;
    }

    /**
     * Sets the value of the listOfCountryNamesByNameResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfCountryNames }
     *     
     */
    public void setListOfCountryNamesByNameResult(ArrayOfCountryNames value) {
        this.listOfCountryNamesByNameResult = value;
    }

}
