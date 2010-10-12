/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class HostModel extends AbstractModel<HostModel> {

    private static final long serialVersionUID = 7667892965813702351L;

    public static final String DEFAULT_NAME;
    static {
        try {
            DEFAULT_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private final Set<String> extensions = new HashSet<String>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final NavigableMap<String, ServerElement> servers = new TreeMap<String, ServerElement>();
    private final NavigableMap<String, JvmElement> jvms = new TreeMap<String, JvmElement>();
    private String name;
    private LocalDomainControllerElement localDomainController;
    private RemoteDomainControllerElement remoteDomainController;
    private ManagementElement managementElement;

    private final PropertiesElement systemProperties = new PropertiesElement(Element.PROPERTY, true);

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.HOST.getLocalName());

    /**
     * Construct a new instance.
     */
    public HostModel() {
        super(ELEMENT_NAME);
    }

    /**
     * Gets the host level configuration for the jvm with the given <code>name</name>.
     * This configuration can extend or override any configuration for a jvm
     * with the same name at the {@link ServerGroupElement#getJvm() server group level}.
     * In turn, the details of the configuration of this jvm can be overridden at the
     * {@link ServerElement#getJvm() server level}.
     *
     * @param name the name of the jvm
     * @return the jvm configuration, or <code>null</code> if there is none with
     *         the given <code>name</name>
     */
    public JvmElement getJvm(String name) {
        return jvms.get(name);
    }

    /**
     * Gets the host level configuration for the interface with the given <code>name</name>.
     * This configuration can override any configuration for an interface
     * with the same name at the {@link DomainModel#getInterface(String) domain level}.
     * In turn, the details of the configuration of this interface can be overridden at the
     * {@link ServerElement#getInterfaces() server level}.
     *
     * @param name the name of the interface
     * @return the interface configuration, or <code>null</code> if there is none with
     *         the given <code>name</name>
     */
    public InterfaceElement getInterface(String name) {
        synchronized (interfaces) {
            return interfaces.get(name);
        }
    }

    /**
     * Gets the named interfaces configured at the host level.
     *
     * @return the interfaces. May be empty but will not be <code>null</code>
     */
    public Set<InterfaceElement> getInterfaces() {
        synchronized (interfaces) {
            return new HashSet<InterfaceElement>(interfaces.values());
        }
    }

    /**
     * Gets the server-specific configurations for the servers associated with this host.
     *
     * @return the servers. May be empty but will not be <code>null</code>
     */
    public Set<ServerElement> getServers() {
        synchronized (servers) {
            return new HashSet<ServerElement>(servers.values());
        }
    }

    /**
     * Gets the server configuration for the server with the given
     * <code>name</code>.
     *
     * @param name the name of the server
     * @return the server configuration, or <code>null</code> if no server
     *         named <code>name</code> is configured
     */
    public ServerElement getServer(String name) {
        synchronized (servers) {
            return servers.get(name);
        }
    }

    /**
     * Gets any system properties defined at the host level. These properties
     * can extend and override any properties declared at the
     * {@link DomainModel#getSystemProperties() domain level} or the
     * {@link ServerGroupElement server group level} and may in turn be extended
     * or overridden by any properties declared at the
     * {@link ServerElement#getSystemProperties() server level}.
     *
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    public LocalDomainControllerElement getLocalDomainControllerElement() {
        return localDomainController;
    }

    public RemoteDomainControllerElement getRemoteDomainControllerElement() {
        return remoteDomainController;
    }

    public ManagementElement getManagementElement() {
        return managementElement;
    }

    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<HostModel> getElementClass() {
        return HostModel.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        writeNamespaces(streamWriter);

        if (systemProperties != null) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
            streamWriter.writeEndElement();
        }

        streamWriter.writeStartElement(Element.DOMAIN_CONTROLLER.getLocalName());
        if (localDomainController != null) {
            streamWriter.writeStartElement(Element.LOCAL.getLocalName());
            localDomainController.writeContent(streamWriter);
        }
        else if (remoteDomainController != null) {
            streamWriter.writeStartElement(Element.REMOTE.getLocalName());
            remoteDomainController.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();

        if (!interfaces.isEmpty()) {
            streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
            for (InterfaceElement element : interfaces.values()) {
                streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (!jvms.isEmpty()) {
            streamWriter.writeStartElement(Element.JVMS.getLocalName());
            for (JvmElement element : jvms.values()) {
                streamWriter.writeStartElement(Element.JVM.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (!servers.isEmpty()) {
            streamWriter.writeStartElement(Element.SERVERS.getLocalName());
            for (ServerElement server : servers.values()) {
                streamWriter.writeStartElement(Element.SERVER.getLocalName());
                server.writeContent(streamWriter);
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }

    boolean addExtension(final String name) {
        return extensions.add(name);
    }

    boolean removeExtension(final String name) {
        return extensions.remove(name);
    }

    Set<String> getExtensions() {
        return extensions;
    }

    InterfaceElement addInterface(final String name) {
        if(interfaces.containsKey(name)) {
            return null;
        }
        final InterfaceElement networkInterface = new InterfaceElement(name);
        interfaces.put(name, networkInterface);
        return networkInterface;
    }

    boolean removeInterface(final String name) {
        return interfaces.remove(name) != null;
    }

    void setName(String name) {
        this.name = name;
    }

    boolean addJvm(String jvmName) {
        if (jvms.containsKey(jvmName))
            return false;
        jvms.put(jvmName, new JvmElement(jvmName));
        return true;
    }

    boolean removeJvm(String jvmName) {
        return jvms.remove(jvmName) != null;
    }

    boolean addManagementElement(String interfaceName, int port) {
        if (managementElement != null)
            return false;
        managementElement = new ManagementElement(interfaceName, port);
        return true;
    }

    boolean removeManagementElement() {
        if (managementElement != null) {
            managementElement = null;
            return true;
        }
        return false;
    }

    boolean addRemoteDomainController(String host, int port) {
        if (localDomainController != null || remoteDomainController != null)
            return false;
        remoteDomainController = new RemoteDomainControllerElement(host, port);
        return true;
    }

    boolean removeRemoteDomainController() {
        if (remoteDomainController == null)
            return false;
        remoteDomainController = null;
        return true;
    }

    boolean addLocalDomainController() {
        if (localDomainController != null || remoteDomainController != null)
            return false;
        localDomainController = new LocalDomainControllerElement();
        return true;
    }

    boolean removeLocalDomainController() {
        if (localDomainController == null)
            return false;
        localDomainController = null;
        return true;
    }

    boolean addServer(String serverName, String groupName) {
        if (servers.containsKey(serverName))
            return false;
        servers.put(serverName, new ServerElement(serverName, groupName));
        return true;
    }

    boolean removeServer(String serverName) {
        return servers.remove(serverName) != null;
    }
}
