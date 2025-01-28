
package org.w3._2001.xmlschema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for typeDerivationControl.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="typeDerivationControl"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}derivationControl"&gt;
 *     &lt;enumeration value="extension"/&gt;
 *     &lt;enumeration value="restriction"/&gt;
 *     &lt;enumeration value="list"/&gt;
 *     &lt;enumeration value="union"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "typeDerivationControl")
@XmlEnum(DerivationControl.class)
public enum TypeDerivationControl {

    @XmlEnumValue("extension")
    EXTENSION(DerivationControl.EXTENSION),
    @XmlEnumValue("restriction")
    RESTRICTION(DerivationControl.RESTRICTION),
    @XmlEnumValue("list")
    LIST(DerivationControl.LIST),
    @XmlEnumValue("union")
    UNION(DerivationControl.UNION);
    private final DerivationControl value;

    TypeDerivationControl(DerivationControl v) {
        value = v;
    }

    public DerivationControl value() {
        return value;
    }

    public static TypeDerivationControl fromValue(DerivationControl v) {
        for (TypeDerivationControl c: TypeDerivationControl.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

}
