/**
 *
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Binding configuration for a named socket.
 *
 * @author Brian Stansberry
 */
public class SocketBindingElement extends AbstractModelElement<SocketBindingElement> {

    private static final long serialVersionUID = 6868487634991345679L;

    private final String name;
    private int port;
    private boolean fixedPort;
    private int multicastPort;
    private InetAddress multicastAddress;
    private String interfaceName;
    private String defaultInterfaceName;

    /**
     * Construct a new instance.
     *
     * @param name the name of the group. Cannot be <code>null</code>
     * @oaram defaultInterfaceName the default interface name. Cannot be <code>null</code>
     */
    public SocketBindingElement(final String name, final String defaultInterfaceName) {
        if(name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        if (defaultInterfaceName == null) {
            throw new IllegalArgumentException("defaultInterfaceName is null");
        }
        this.defaultInterfaceName = defaultInterfaceName;
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
     * Gets the name of the interface to use for this socket binding. This is
     * either the {@link #getConfiguredInterfaceName() configured interface name}
     * or the containing socket binding group's
     * {@link #getDefaultInterfaceName() default interface name}.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getInterfaceName() {
        return interfaceName == null ? defaultInterfaceName : interfaceName;
    }

    /**
     * Gets the name of the interface specificially configured for use for this
     * socket binding, or <code>null</code> if no specific configuration was
     * supplied and this socket binding should use the
     * {@link #getDefaultInterfaceName() socket binding group's default interface}.
     *
     * @return the name. May be <code>null</code>
     */
    public String getConfiguredInterfaceName() {
        return interfaceName;
    }

    /**
     * Gets the name of the default interface to use for this socket binding if
     * no specific interface was {@link #getConfiguredInterfaceName() configured}.
     * This value comes from the enclosing {@link SocketBindingGroupElement}.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getDefaultInterfaceName() {
        return defaultInterfaceName;
    }

    void setDefaultInterfaceName(String defaultInterfaceName) {
        this.defaultInterfaceName = defaultInterfaceName;
    }

    /**
     * Sets the name of the interface to use for this socket binding.
     *
     * @param interfaceName the name. May be <code>null</code>
     */
    void setConfiguredInterfaceName(String interfaceName) {
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

}
