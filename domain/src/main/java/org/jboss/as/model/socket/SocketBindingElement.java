/**
 * 
 */
package org.jboss.as.model.socket;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.RefResolver;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

/**
 * Binding configuration for a named socket.
 * 
 * @author Brian Stansberry
 */
public class SocketBindingElement extends AbstractModelElement<SocketBindingElement> {

    private static final long serialVersionUID = 6868487634991345679L;

    private final String name;
    private final RefResolver<String, InterfaceElement> interfaceResolver;
    private String interfaceName;
    private int port;
    private boolean fixedPort;
    private InetAddress multicastAddress;
    private int multicastPort;
    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the element
     * @param name the name of the group. Cannot be <code>null</code>
     * @param interfaceResolver {@link RefResolver} to use to resolve references 
     *           to interfaces. May be used safely in the constructor
     *           itself. Cannot be <code>null</code>
     */
    public SocketBindingElement(Location location, final String name, final RefResolver<String, InterfaceElement> interfaceResolver) {
        super(location);
        this.name = name;
        
        if (interfaceResolver == null)
            throw new IllegalArgumentException("interfaceResolver is null");
        this.interfaceResolver = interfaceResolver;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @param interfaceResolver {@link RefResolver} to use to resolve references 
     *           to interfaces. May be used safely in the constructor
     *           itself. Cannot be <code>null</code>
     * @throws XMLStreamException if an error occurs
     */
    public SocketBindingElement(XMLExtendedStreamReader reader, final RefResolver<String, InterfaceElement> interfaceResolver) throws XMLStreamException {
        super(reader);
        
        if (interfaceResolver == null)
            throw new IllegalArgumentException("interfaceResolver is null");
        this.interfaceResolver = interfaceResolver;
        
        // Handle attributes
        String name = null;
        String intf = null;
        Integer port = null;
        Boolean fixPort = null;
        InetAddress mcastAddr = null;
        Integer mcastPort = null;
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
                    case INTERFACE: {
                        if (this.interfaceResolver.resolveRef(value) == null) {
                            throw new XMLStreamException("Unknown interface " + value + 
                                    " " + attribute.getLocalName() + " must be declared in element " + 
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        intf = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.valueOf(parsePort(value, attribute, reader, true));
                        break;
                    }
                    case FIXED_PORT: {
                        fixPort = Boolean.valueOf(value);
                        break;
                    }
                    case MULTICAST_ADDRESS: {
                        try {
                            mcastAddr = InetAddress.getByName(value);
                            if (!mcastAddr.isMulticastAddress()) {
                                throw new XMLStreamException("Value " + value + " for attribute " + 
                                        attribute.getLocalName() + " is not a valid multicast address", 
                                        reader.getLocation());
                            }
                        } catch (UnknownHostException e) {
                            throw new XMLStreamException("Value " + value + " for attribute " + 
                                    attribute.getLocalName() + " is not a valid multicast address", 
                                    reader.getLocation(), e);
                        }
                    }
                    case MULTICAST_PORT: {
                        mcastPort = Integer.valueOf(parsePort(value, attribute, reader, false));
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        this.interfaceName = intf;
        this.port = port == null ? 0 : port;
        this.fixedPort = fixPort == null ? false : fixPort.booleanValue();
        this.multicastAddress = mcastAddr;
        this.multicastPort = mcastAddr == null ? -1 : mcastPort != null ? mcastPort.intValue() : -1;
        if (this.multicastPort == -1 && this.multicastAddress != null) {
            throw new XMLStreamException("Must configure " + Attribute.MULTICAST_PORT + 
                    " if " + Attribute.MULTICAST_ADDRESS + " is configured", reader.getLocation());
        }
        // Handle elements
        requireNoContent(reader);
    }
    
    /**
     * Gets the name of the socket binding
     * 
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the name of the interface to use for this socket binding.
     * 
     * @return the name. May be <code>null</code>
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * Sets the name of the interface to use for this socket binding.
     * 
     * @param interfaceName the name. May be <code>null</code>
     */
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * Gets the port to use for this socket binding.
     * 
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port to use for this socket binding.
     * 
     * @param port the port
     */
    void setPort(int port) {
        if (multicastPort < 0 || multicastPort >= 65536)
            throw new IllegalArgumentException("multicastPort must be between 1 and 65536");
        this.port = port;
    }

    /**
     * Gets whether the specified port should not be overridden via a 
     * server-level port offset.
     * 
     * @return <code>true</code> if the port should not be overridden; <code>false</code>
     *         if overriding is allowed
     */
    public boolean isFixedPort() {
        return fixedPort;
    }

    /**
     * Sets whether the specified port should not be overridden via a 
     * server-level port offset.
     * 
     * @param fixedPort <code>true</code> if the port should not be overridden; 
     *                  <code>false</code> if overriding is allowed
     */
    void setFixedPort(boolean fixedPort) {
        this.fixedPort = fixedPort;
    }

    /**
     * Gets the multicast address (if any) on which this socket should listen
     * for multicast traffic
     * 
     * @return the multicast address, or <code>null</code> if the socket should
     *         not listen for multicast
     */
    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }

    /**
     * Sets the multicast address (if any) on which this socket should listen
     * for multicast traffic
     * 
     * @param  multicastAddress the multicast address, or <code>null</code> if the socket should
     *         not listen for multicast
     */
    void setMulticastAddress(InetAddress multicastAddress) {
        if (multicastAddress != null && !multicastAddress.isMulticastAddress()) {
            throw new IllegalArgumentException(multicastAddress + " is not a multicast address");
        }
        this.multicastAddress = multicastAddress;
        if (this.multicastAddress == null) {
            this.multicastPort = -1;
        }
    }

    /**
     * Gets the multicast port (if any) on which this socket should listen
     * for multicast traffic
     * 
     * @return the multicast port, or <code>-1</code> if the socket should
     *         not listen for multicast
     */
    public int getMulticastPort() {
        return multicastPort;
    }

    /**
     * Sets the multicast port (if any) on which this socket should listen
     * for multicast traffic
     * 
     * @param multicastPort the multicast port
     */
    void setMulticastPort(int multicastPort) {
        if (multicastPort < 1 || multicastPort >= 65536)
            throw new IllegalArgumentException("multicastPort must be between 1 and 65536");
        this.multicastPort = multicastPort;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#appendDifference(java.util.Collection, org.jboss.as.model.AbstractModelElement)
     */
    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SocketBindingElement>> target,
            SocketBindingElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#elementHash()
     */
    @Override
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        if (interfaceName != null)
            cksum = Long.rotateLeft(cksum, 1) ^ interfaceName.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ port & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ Boolean.valueOf(fixedPort).hashCode() & 0xffffffffL;
        if (multicastAddress != null)
            cksum = Long.rotateLeft(cksum, 1) ^ multicastAddress.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ multicastPort & 0xffffffffL;
        return cksum;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<SocketBindingElement> getElementClass() {
        return SocketBindingElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (interfaceName != null) {
            streamWriter.writeAttribute(Attribute.INTERFACE.getLocalName(), interfaceName);
        }
        if (port != 0) {
            streamWriter.writeAttribute(Attribute.PORT.getLocalName(), String.valueOf(port));
        }
        if (fixedPort) {
            streamWriter.writeAttribute(Attribute.FIXED_PORT.getLocalName(), "true");
        }
        if (multicastAddress != null) {
            streamWriter.writeAttribute(Attribute.MULTICAST_ADDRESS.getLocalName(), multicastAddress.getHostAddress());
        }
        if (multicastPort != -1) {
            streamWriter.writeAttribute(Attribute.MULTICAST_PORT.getLocalName(), String.valueOf(multicastPort));
        }
        streamWriter.writeEndElement();
    }
    
    private int parsePort(String value, Attribute attribute, XMLExtendedStreamReader reader, boolean allowEphemeral) throws XMLStreamException {
        int legal;
        try {
            legal = Integer.valueOf(value);
            int min = allowEphemeral ? 0 : 1;
            if (legal < min || legal >= 65536) {
                throw new XMLStreamException("Illegal value " + value + 
                        " for attribute '" + attribute.getLocalName() + 
                        "' must be between " + min + " and 65536", reader.getLocation());
            }
        }
        catch (NumberFormatException nfe) {
            throw new XMLStreamException("Illegal value " + value + 
                    " for attribute '" + attribute.getLocalName() + 
                    "' must be an integer", reader.getLocation(), nfe);
        }
        return legal;
    }

}
