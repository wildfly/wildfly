/**
 *
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.interfaces.SubnetMatchInterfaceCriteria;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that an address must fit on a particular subnet to match the criteria.
 *
 * @author Brian Stansberry
 */
public class SubnetMatchCriteriaElement extends AbstractInterfaceCriteriaElement<SubnetMatchCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;

    private String value;
    private byte[] network;
    private int mask;

    /**
     * Create a new SubnetMatchCriteriaElement.
     *
     * @param value
     * @param network
     * @param mask
     */
    public SubnetMatchCriteriaElement(String value, byte[] network, int mask) {
        super(Element.SUBNET_MATCH);
        this.value = value;
        this.network = network;
        this.mask = mask;
        setInterfaceCriteria(new SubnetMatchInterfaceCriteria(this.network, this.mask));
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), value);
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<SubnetMatchCriteriaElement> getElementClass() {
        return SubnetMatchCriteriaElement.class;
    }

}
