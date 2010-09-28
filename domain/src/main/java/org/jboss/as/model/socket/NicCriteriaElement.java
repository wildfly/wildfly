/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
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
     * Creates a new NicCriteriaElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public NicCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.NIC);

        this.name = ParseUtils.readStringAttributeElement(reader, Attribute.NAME.getLocalName());

        setInterfaceCriteria(new NicInterfaceCriteria(name));
    }

    @Override
    private long elementHash() {
        return name.hashCode() & 0xffffffffL;
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
