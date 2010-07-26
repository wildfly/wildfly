/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
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
public class SubnetMatchCriteriaElement extends AbstractInterfaceCriteriaElement<SubnetMatchCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;

    private String value;
    private byte[] network;
    private int mask;
    
    /**
     * Creates a new SubnetMatchCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public SubnetMatchCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.SUBNET_MATCH);
    }

    @Override
    protected void processXmlStream(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String value = null;
        byte[] net = null;
        int mask = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        String[] split = null;
                        try {
                            split = value.split("/");
                            if (split.length != 2) {
                                throw new XMLStreamException("Invalid 'value' " + value + " -- must be of the form address/mask", reader.getLocation());
                            }
                            InetAddress addr = InetAddress.getByName(split[1]);
                            net = addr.getAddress();
                            mask = Integer.valueOf(split[1]);
                        }
                        catch (NumberFormatException e) {
                            throw new XMLStreamException("Invalid mask " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        catch (UnknownHostException e) {
                            throw new XMLStreamException("Invalid address " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (net == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        this.value = value;
        this.network = net;
        this.mask = mask;
        // Handle elements
        requireNoContent(reader);
    }

    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        byte[] addr = address.getAddress();
        if (addr.length != network.length) {
            // different address type TODO translate?
            return false;
        }
        int last = addr.length - mask;
        for (int i = 0; i < last; i++) {
            if (addr[i] != network[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long elementHash() {
        return value.hashCode() & 0xffffffffL;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), value);
        streamWriter.writeEndElement();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SubnetMatchCriteriaElement>> target, SubnetMatchCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<SubnetMatchCriteriaElement> getElementClass() {
        return SubnetMatchCriteriaElement.class;
    }

}
