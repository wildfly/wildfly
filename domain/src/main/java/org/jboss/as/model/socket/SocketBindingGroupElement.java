/**
 * 
 */
package org.jboss.as.model.socket;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A named group of socket binding configurations that can be applied to a
 * server group.
 * 
 * @author Brian Stansberry
 */
public class SocketBindingGroupElement extends AbstractModelElement<SocketBindingGroupElement> {

    private static final long serialVersionUID = -7389975620327080290L;

    private final String name;
    private final String defaultInterface;
    private final NavigableMap<String, SocketBindingGroupIncludeElement> includedGroups = new TreeMap<String, SocketBindingGroupIncludeElement>();
    private final NavigableMap<String, SocketBindingElement> socketBindings = new TreeMap<String, SocketBindingElement>();
    
    
    /**
     * @param location
     */
    public SocketBindingGroupElement(Location location, final String name, final String defaultInterface) {
        super(location);
        this.name = name;
        this.defaultInterface = defaultInterface;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public SocketBindingGroupElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        String defIntf = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case DEFAULT_INTERFACE: {
                        defIntf = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (defIntf == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.DEFAULT_INTERFACE));
        }
        this.name = name;
        this.defaultInterface = defIntf;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            final SocketBindingGroupIncludeElement include = new SocketBindingGroupIncludeElement(reader);
                            if (includedGroups.containsKey(include.getGroupName())) {
                                throw new XMLStreamException("Included socket-binding-group " + include.getGroupName() + " already declared", reader.getLocation());
                            }
                            includedGroups.put(include.getGroupName(), include);
                            break;
                        }
                        case SOCKET_BINDING: {
                            final SocketBindingElement include = new SocketBindingElement(reader);
                            if (socketBindings.containsKey(include.getName())) {
                                throw new XMLStreamException("socket-binding " + include.getName() + " already declared", reader.getLocation());
                            }
                            socketBindings.put(include.getName(), include);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }
    
    /**
     * Gets the name of the socket binding group.
     * 
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of the default interface to use for socket bindings
     * that do not declare an interface.
     * 
     * @return the interface name
     */
    public String getDefaultInterface() {
        return defaultInterface;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#appendDifference(java.util.Collection, org.jboss.as.model.AbstractModelElement)
     */
    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SocketBindingGroupElement>> target,
            SocketBindingGroupElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#elementHash()
     */
    @Override
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ defaultInterface.hashCode() & 0xffffffffL;
        cksum = calculateElementHashOf(includedGroups.values(), cksum);
        cksum = calculateElementHashOf(socketBindings.values(), cksum);
        return cksum;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<SocketBindingGroupElement> getElementClass() {
        return SocketBindingGroupElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.DEFAULT_INTERFACE.getLocalName(), defaultInterface);
        for (SocketBindingGroupIncludeElement included : includedGroups.values()) {
            streamWriter.writeStartElement(Element.INCLUDE.getLocalName());
            included.writeContent(streamWriter);
        }for (SocketBindingElement included : socketBindings.values()) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING.getLocalName());
            included.writeContent(streamWriter);
        }
    }

}
