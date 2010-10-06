/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that a network interface must have a particular {@link NetworkInterface#getName() name}
 * in order to match the criteria.
 *
 * @author Brian Stansberry
 */
public class NicCriteriaElement extends AbstractInterfaceCriteriaElement<NicCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;
    private String name;

    /**
     * Create a new NicCriteriaElement.
     *
     *  @param the nic name
     */
    public NicCriteriaElement(final String name) {
        super(Element.NIC);
        this.name = name;
        setInterfaceCriteria(new NicInterfaceCriteria(name));
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<NicCriteriaElement> getElementClass() {
        return NicCriteriaElement.class;
    }

}
