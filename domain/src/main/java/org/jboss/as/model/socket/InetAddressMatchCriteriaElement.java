/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that a network interface must have a {@link NetworkInterface#getName() name}
 * that matches a given regular expression in order to match the criteria.
 *
 * @author Brian Stansberry
 */
public class InetAddressMatchCriteriaElement extends AbstractInterfaceCriteriaElement<InetAddressMatchCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;

    private String address;

    /**
     * Creates a new NicMatchCriteriaElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public InetAddressMatchCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.INET_ADDRESS);

        String address = readStringAttributeElement(reader, Attribute.VALUE.getLocalName());
        setInterfaceCriteria(new InetAddressMatchInterfaceCriteria(address));
    }

    @Override
    public long elementHash() {
        return address.hashCode() & 0xffffffffL;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), address);
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<InetAddressMatchCriteriaElement> getElementClass() {
        return InetAddressMatchCriteriaElement.class;
    }

}
