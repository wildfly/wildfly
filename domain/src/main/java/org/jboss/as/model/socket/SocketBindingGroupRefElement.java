/**
 * 
 */
package org.jboss.as.model.socket;

import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapping of a socket binding group to a server group or server.
 * 
 * @author Brian Stansberry
 */
public class SocketBindingGroupRefElement extends AbstractModelElement<SocketBindingGroupRefElement> {

    private static final long serialVersionUID = 4250004070765682113L;

    private final String ref;
    private int portOffset;
    
    /**
     * @param location
     */
    public SocketBindingGroupRefElement(Location location, final String ref) {
        super(location);
        this.ref  =ref;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public SocketBindingGroupRefElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        int offset = 0;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case REF: {
                        name = value;
                        break;
                    }
                    case PORT_OFFSET: {
                        try {
                            offset = Integer.valueOf(value);
                            if (offset < 0) {
                                throw new XMLStreamException(offset + " is not a valid " + 
                                        attribute.getLocalName() + " -- must be greater than zero", 
                                        reader.getLocation());
                            }
                        } catch (NumberFormatException e) {
                            throw new XMLStreamException(offset + " is not a valid " + 
                                    attribute.getLocalName(), reader.getLocation(), e);
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.REF));
        }
        this.ref = name;
        this.portOffset = offset;
        // Handle elements
        requireNoContent(reader);
    }
    
    
    /**
     * Gets the offset to apply to ports in the socket binding group to get
     * port values that are unused on a server.
     * 
     * @return a non-negative number
     */
    public int getPortOffset() {
        return portOffset;
    }

    /**
     * Sets the offset to apply to ports in the socket binding group to get
     * port values that are unused on a server.
     * 
     * @param portOffset a non-negative number
     */
    void setPortOffset(int portOffset) {
        if (portOffset < 0) {
            throw new IllegalArgumentException(portOffset + " is not a valid " + 
                    "port offset -- must be greater than zero");
        }
        this.portOffset = portOffset;
    }

    /**
     * Gets the name of the socket binding group.
     * 
     * @return the name. Will not be <code>null</code>
     */
    public String getRef() {
        return ref;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#appendDifference(java.util.Collection, org.jboss.as.model.AbstractModelElement)
     */
    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SocketBindingGroupRefElement>> target,
            SocketBindingGroupRefElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#elementHash()
     */
    @Override
    public long elementHash() {
        long cksum = ref.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ portOffset & 0xffffffffL;
        return cksum;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<SocketBindingGroupRefElement> getElementClass() {
        return SocketBindingGroupRefElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.REF.getLocalName(), ref);
        if (portOffset != 0) {
            streamWriter.writeAttribute(Attribute.PORT_OFFSET.getLocalName(), String.valueOf(portOffset));
        }

    }

}
