
package https.soap_service_free_mock_beeceptor_com.countryinfoservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the https.soap_service_free_mock_beeceptor_com.countryinfoservice package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ListOfContinentsByName_QNAME = new QName("https://soap-service-free.mock.beeceptor.com/CountryInfoService", "ListOfContinentsByName");
    private final static QName _ListOfContinentsByNameResponse_QNAME = new QName("https://soap-service-free.mock.beeceptor.com/CountryInfoService", "ListOfContinentsByNameResponse");
    private final static QName _ListOfCountryNamesByName_QNAME = new QName("https://soap-service-free.mock.beeceptor.com/CountryInfoService", "ListOfCountryNamesByName");
    private final static QName _ListOfCountryNamesByNameResponse_QNAME = new QName("https://soap-service-free.mock.beeceptor.com/CountryInfoService", "ListOfCountryNamesByNameResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: https.soap_service_free_mock_beeceptor_com.countryinfoservice
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ListOfContinentsByNameType }
     * 
     */
    public ListOfContinentsByNameType createListOfContinentsByNameType() {
        return new ListOfContinentsByNameType();
    }

    /**
     * Create an instance of {@link ListOfContinentsByNameResponseType }
     * 
     */
    public ListOfContinentsByNameResponseType createListOfContinentsByNameResponseType() {
        return new ListOfContinentsByNameResponseType();
    }

    /**
     * Create an instance of {@link ListOfCountryNamesByNameType }
     * 
     */
    public ListOfCountryNamesByNameType createListOfCountryNamesByNameType() {
        return new ListOfCountryNamesByNameType();
    }

    /**
     * Create an instance of {@link ListOfCountryNamesByNameResponseType }
     * 
     */
    public ListOfCountryNamesByNameResponseType createListOfCountryNamesByNameResponseType() {
        return new ListOfCountryNamesByNameResponseType();
    }

    /**
     * Create an instance of {@link ArrayOfContinents }
     * 
     */
    public ArrayOfContinents createArrayOfContinents() {
        return new ArrayOfContinents();
    }

    /**
     * Create an instance of {@link ArrayOfCountryNames }
     * 
     */
    public ArrayOfCountryNames createArrayOfCountryNames() {
        return new ArrayOfCountryNames();
    }

    /**
     * Create an instance of {@link Continent }
     * 
     */
    public Continent createContinent() {
        return new Continent();
    }

    /**
     * Create an instance of {@link CountryCodeAndName }
     * 
     */
    public CountryCodeAndName createCountryCodeAndName() {
        return new CountryCodeAndName();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListOfContinentsByNameType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListOfContinentsByNameType }{@code >}
     */
    @XmlElementDecl(namespace = "https://soap-service-free.mock.beeceptor.com/CountryInfoService", name = "ListOfContinentsByName")
    public JAXBElement<ListOfContinentsByNameType> createListOfContinentsByName(ListOfContinentsByNameType value) {
        return new JAXBElement<ListOfContinentsByNameType>(_ListOfContinentsByName_QNAME, ListOfContinentsByNameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListOfContinentsByNameResponseType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListOfContinentsByNameResponseType }{@code >}
     */
    @XmlElementDecl(namespace = "https://soap-service-free.mock.beeceptor.com/CountryInfoService", name = "ListOfContinentsByNameResponse")
    public JAXBElement<ListOfContinentsByNameResponseType> createListOfContinentsByNameResponse(ListOfContinentsByNameResponseType value) {
        return new JAXBElement<ListOfContinentsByNameResponseType>(_ListOfContinentsByNameResponse_QNAME, ListOfContinentsByNameResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListOfCountryNamesByNameType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListOfCountryNamesByNameType }{@code >}
     */
    @XmlElementDecl(namespace = "https://soap-service-free.mock.beeceptor.com/CountryInfoService", name = "ListOfCountryNamesByName")
    public JAXBElement<ListOfCountryNamesByNameType> createListOfCountryNamesByName(ListOfCountryNamesByNameType value) {
        return new JAXBElement<ListOfCountryNamesByNameType>(_ListOfCountryNamesByName_QNAME, ListOfCountryNamesByNameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListOfCountryNamesByNameResponseType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListOfCountryNamesByNameResponseType }{@code >}
     */
    @XmlElementDecl(namespace = "https://soap-service-free.mock.beeceptor.com/CountryInfoService", name = "ListOfCountryNamesByNameResponse")
    public JAXBElement<ListOfCountryNamesByNameResponseType> createListOfCountryNamesByNameResponse(ListOfCountryNamesByNameResponseType value) {
        return new JAXBElement<ListOfCountryNamesByNameResponseType>(_ListOfCountryNamesByNameResponse_QNAME, ListOfCountryNamesByNameResponseType.class, null, value);
    }

}
