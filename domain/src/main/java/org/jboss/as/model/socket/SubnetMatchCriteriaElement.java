/**
 *
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
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
     * Creates a new SubnetMatchCriteriaElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public SubnetMatchCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(Element.SUBNET_MATCH);

        // Handle attributes
        String value = null;
        byte[] net = null;
        int mask = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
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
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (net == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        this.value = value;
        this.network = net;
        this.mask = mask;
        // Handle elements
        ParseUtils.requireNoContent(reader);

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
