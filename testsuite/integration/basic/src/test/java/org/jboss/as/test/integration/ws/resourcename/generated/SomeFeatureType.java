
package org.jboss.as.test.integration.ws.resourcename.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour SomeFeatureType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="SomeFeatureType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="someMeasure">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *               &lt;minInclusive value="0"/>
 *               &lt;maxInclusive value="9"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SomeFeatureType", propOrder = {
    "someMeasure"
})
public class SomeFeatureType {

    protected int someMeasure;

    /**
     * Obtient la valeur de la propriété someMeasure.
     * 
     */
    public int getSomeMeasure() {
        return someMeasure;
    }

    /**
     * Définit la valeur de la propriété someMeasure.
     * 
     */
    public void setSomeMeasure(int value) {
        this.someMeasure = value;
    }

}
