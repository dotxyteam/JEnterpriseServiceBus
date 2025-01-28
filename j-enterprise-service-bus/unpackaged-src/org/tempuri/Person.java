
package org.tempuri;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Person complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Person"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="SSN" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="DOB" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *         &lt;element name="Home" type="{http://tempuri.org}Address" minOccurs="0"/&gt;
 *         &lt;element name="Office" type="{http://tempuri.org}Address" minOccurs="0"/&gt;
 *         &lt;element name="Spouse" type="{http://tempuri.org}Person" minOccurs="0"/&gt;
 *         &lt;element name="FavoriteColors" type="{http://tempuri.org}ArrayOfFavoriteColorsItemString" minOccurs="0"/&gt;
 *         &lt;element name="Age" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Person", propOrder = {
    "name",
    "ssn",
    "dob",
    "home",
    "office",
    "spouse",
    "favoriteColors",
    "age"
})
@XmlSeeAlso({
    Employee.class
})
public class Person {

    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "SSN", required = true)
    protected String ssn;
    @XmlElement(name = "DOB")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar dob;
    @XmlElement(name = "Home")
    protected Address home;
    @XmlElement(name = "Office")
    protected Address office;
    @XmlElement(name = "Spouse")
    protected Person spouse;
    @XmlElement(name = "FavoriteColors")
    protected ArrayOfFavoriteColorsItemString favoriteColors;
    @XmlElement(name = "Age")
    protected Long age;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the ssn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSSN() {
        return ssn;
    }

    /**
     * Sets the value of the ssn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSSN(String value) {
        this.ssn = value;
    }

    /**
     * Gets the value of the dob property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDOB() {
        return dob;
    }

    /**
     * Sets the value of the dob property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDOB(XMLGregorianCalendar value) {
        this.dob = value;
    }

    /**
     * Gets the value of the home property.
     * 
     * @return
     *     possible object is
     *     {@link Address }
     *     
     */
    public Address getHome() {
        return home;
    }

    /**
     * Sets the value of the home property.
     * 
     * @param value
     *     allowed object is
     *     {@link Address }
     *     
     */
    public void setHome(Address value) {
        this.home = value;
    }

    /**
     * Gets the value of the office property.
     * 
     * @return
     *     possible object is
     *     {@link Address }
     *     
     */
    public Address getOffice() {
        return office;
    }

    /**
     * Sets the value of the office property.
     * 
     * @param value
     *     allowed object is
     *     {@link Address }
     *     
     */
    public void setOffice(Address value) {
        this.office = value;
    }

    /**
     * Gets the value of the spouse property.
     * 
     * @return
     *     possible object is
     *     {@link Person }
     *     
     */
    public Person getSpouse() {
        return spouse;
    }

    /**
     * Sets the value of the spouse property.
     * 
     * @param value
     *     allowed object is
     *     {@link Person }
     *     
     */
    public void setSpouse(Person value) {
        this.spouse = value;
    }

    /**
     * Gets the value of the favoriteColors property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfFavoriteColorsItemString }
     *     
     */
    public ArrayOfFavoriteColorsItemString getFavoriteColors() {
        return favoriteColors;
    }

    /**
     * Sets the value of the favoriteColors property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfFavoriteColorsItemString }
     *     
     */
    public void setFavoriteColors(ArrayOfFavoriteColorsItemString value) {
        this.favoriteColors = value;
    }

    /**
     * Gets the value of the age property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getAge() {
        return age;
    }

    /**
     * Sets the value of the age property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setAge(Long value) {
        this.age = value;
    }

}
