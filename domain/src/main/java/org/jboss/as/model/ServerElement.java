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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * An individual server on a {@link HostModel}.
 *
 * @author Brian Stansberry
 */
public final class ServerElement extends AbstractModelElement<ServerElement> {

    private static final long serialVersionUID = 7667892965813702351L;

    private final String name;
    private final String serverGroup;
    private final NavigableMap<String, ServerInterfaceElement> interfaces = new TreeMap<String, ServerInterfaceElement>();
    private boolean start;
    private SocketBindingGroupRefElement bindingGroup;
    private JvmElement jvm;
    private PropertiesElement systemProperties;

    /**
     * Construct a new instance.
     *
     */
    public ServerElement(final String name, final String serverGroup) {
        this.name = name;
        this.serverGroup = serverGroup;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public ServerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String name = null;
        String group = null;
        Boolean start = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case GROUP: {
                        group = value;
                        break;
                    }
                    case START: {
                        start = Boolean.valueOf(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (group == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.GROUP));
        }
        this.name = name;
        this.serverGroup = group;
        this.start = start == null ? true : start.booleanValue();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE_SPECS: {
                            parseInterfaces(reader);
                            break;
                        }
                        case JVM: {
                            if (jvm != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            jvm = new JvmElement(reader);
                            break;
                        }
                        case SOCKET_BINDING_GROUP: {
                            if (bindingGroup != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            bindingGroup = new SocketBindingGroupRefElement(reader);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

    }


    /**
     * Gets whether this server should be started.
     *
     * @return <code>true</code> if the server should be started, <code>false</code> if not
     */
    public boolean isStart() {
        return start;
    }

    /**
     * Sets whether this server should be started.
     *
     * @param start <code>true</code> if the server should be started, <code>false</code> if not
     */
    void setStart(boolean start) {
        this.start = start;
    }

    /**
     * Gets the name of the server.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the default jvm configuration for servers in this group. This can
     * be overridden at the {@link ServerElement#getJvm() server level}.
     *
     * @return the jvm configuration, or <code>null</code> if there is none
     */
    public JvmElement getJvm() {
        return jvm;
    }

    /**
     * Gets the default jvm configuration for servers in this group.
     *
     * param jvm the jvm configuration. May be <code>null</code>
     */
    void setJvm(JvmElement jvm) {
        this.jvm = jvm;
    }

    public Set<ServerInterfaceElement> getInterfaces() {
        Set<ServerInterfaceElement> intfs = new LinkedHashSet<ServerInterfaceElement>();
        synchronized (interfaces) {
            for (Map.Entry<String, ServerInterfaceElement> entry : interfaces.entrySet()) {
                intfs.add(entry.getValue());
            }
        }
        return intfs;
    }

    public SocketBindingGroupRefElement getSocketBindingGroup() {
        return bindingGroup;
    }

    /**
     * Gets the name of the server's server group.
     *
     * @return the server group name. Will not be <code>null</code>
     */
    public String getServerGroup() {
        return serverGroup;
    }

    /**
     * Gets any system properties defined at the server level. These properties
     * can extend and override any properties declared at the
     * {@link DomainModel#getSystemProperties() domain level}, the
     * {@link ServerGroupElement server group level} or the
     * {@link ServerElement#getSystemProperties() server level}.
     *
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<ServerElement> getElementClass() {
        return ServerElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        // TODO re-evaluate the element order in the xsd; make sure this is correct

        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.GROUP.getLocalName(), serverGroup);
        if (!start) {
            streamWriter.writeAttribute(Attribute.START.getLocalName(), "false");
        }

        synchronized (interfaces) {
            if (! interfaces.isEmpty()) {
                streamWriter.writeStartElement(Element.INTERFACE_SPECS.getLocalName());
                for (ServerInterfaceElement element : interfaces.values()) {
                    streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        if (bindingGroup != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            bindingGroup.writeContent(streamWriter);
        }

        if (systemProperties != null) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
            streamWriter.writeEndElement();
        }

        if (jvm != null) {
            streamWriter.writeStartElement(Element.JVM.getLocalName());
            jvm.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    private void parseInterfaces(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE: {
                            final ServerInterfaceElement interfaceEl = new ServerInterfaceElement(reader);
                            if (interfaces.containsKey(interfaceEl.getName())) {
                                throw new XMLStreamException("Interface " + interfaceEl.getName() + " already declared", reader.getLocation());
                            }
                            interfaces.put(interfaceEl.getName(), interfaceEl);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }
}
