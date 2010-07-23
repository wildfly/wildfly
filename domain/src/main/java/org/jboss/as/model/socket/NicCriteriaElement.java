/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
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
    }

    @Override
    protected void processXmlStream(XMLExtendedStreamReader reader) throws XMLStreamException {
        this.name = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
    }

    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        return name.equals(networkInterface.getName());
    }

    @Override
    public long elementHash() {
        return name.hashCode() & 0xffffffffL;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeEndElement();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<NicCriteriaElement>> target, NicCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<NicCriteriaElement> getElementClass() {
        return NicCriteriaElement.class;
    }

}
