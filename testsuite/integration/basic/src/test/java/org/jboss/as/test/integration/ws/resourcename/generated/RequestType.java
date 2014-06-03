
package org.jboss.as.test.integration.ws.resourcename.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour RequestType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="RequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="request" type="{http://www.test.nl/xsd/test/messages}SomeFeatureType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequestType", propOrder = {
    "request"
})
public class RequestType {

    @XmlElement(required = true)
    protected SomeFeatureType request;

    /**
     * Obtient la valeur de la propriété request.
     * 
     * @return
     *     possible object is
     *     {@link SomeFeatureType }
     *     
     */
    public SomeFeatureType getRequest() {
        return request;
    }

    /**
     * Définit la valeur de la propriété request.
     * 
     * @param value
     *     allowed object is
     *     {@link SomeFeatureType }
     *     
     */
    public void setRequest(SomeFeatureType value) {
        this.request = value;
    }

}
