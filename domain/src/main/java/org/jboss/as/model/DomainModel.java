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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.SubsystemFactory;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.marshalling.FieldSetter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The JBoss AS Domain state.  An instance of this class represents the complete running state of the domain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainModel extends AbstractModel<DomainModel> {

    private static final long serialVersionUID = 5516070442013067881L;

    // non-model fields
    private final transient Map<String, SubsystemFactory<?>> subsystemTypes = new HashMap<String, SubsystemFactory<?>>();

    private static final FieldSetter subsystemTypesSetter = FieldSetter.get(DomainModel.class, "subsystemTypes");

    // model fields
    private final Set<String> extensions = new HashSet<String>();
    private final NavigableMap<String, ServerGroupElement> serverGroups = new TreeMap<String, ServerGroupElement>();
    private final NavigableMap<String, DeploymentUnitElement> deployments = new TreeMap<String, DeploymentUnitElement>();
    private final NavigableMap<String, ProfileElement> profiles = new TreeMap<String, ProfileElement>();
    private final NavigableMap<String, InterfaceElement> interfaces = new TreeMap<String, InterfaceElement>();
    private final NavigableMap<String, SocketBindingGroupElement> bindingGroups = new TreeMap<String, SocketBindingGroupElement>();

    private PropertiesElement systemProperties;

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.DOMAIN.getLocalName());

    /**
     * Construct a new instance.
     */
    public DomainModel() {
        super(ELEMENT_NAME);
    }

    /**
     * Gets the extension modules available for use in this domain.
     *
     * @return the extensions. May be empty but will not be <code>null</code>
     */
    public Set<String> getExtensions() {
        synchronized (extensions) {
            return new HashSet<String>(extensions);
        }
    }

    /**
     * Gets the named interfaces available for use in this domain.
     *
     * @return the interfaces. May be empty but will not be <code>null</code>
     */
    public Set<InterfaceElement> getInterfaces() {
        Set<InterfaceElement> intfs = new LinkedHashSet<InterfaceElement>();
        synchronized (interfaces) {
            for (Map.Entry<String, InterfaceElement> entry : interfaces.entrySet()) {
                intfs.add(entry.getValue());
            }
        }
        return intfs;
    }

    /**
     * Gets the domain-level configuration for a particular interface. Note that
     * this configuration can be overridden at the {@link ServerGroupElement server group}
     * {@link HostModel host} or {@link ServerElement server} levels.
     *
     * @param name the name of the interface
     * @return the interface configuration, or <code>null</code> if no interface
     *         named <code>name</code> is configured
     */
    public InterfaceElement getInterface(String name) {
        synchronized (interfaces) {
            return interfaces.get(name);
        }
    }

    /**
     * Gets the configuration for a given profile.
     *
     * @param name the name of the profile
     * @return the profile configuration, or <code>null</code> if no profile
     *         named <code>name</code> is configured
     */
    public ProfileElement getProfile(String name) {
        synchronized (profiles) {
            return profiles.get(name);
        }
    }

    /**
     * Gets the socket binding group configuration for the group with the given
     * <code>name</code>.
     *
     * @param name the name of the socket binding group
     * @return the socket binding group configuration, or <code>null</code> if
     *         no socket binding named <code>name</code> is configured
     */
    public SocketBindingGroupElement getSocketBindingGroup(String name) {
        synchronized (bindingGroups) {
            return bindingGroups.get(name);
        }
    }

    /**
     * Gets the server group configuration for the group with the given
     * <code>name</code>.
     *
     * @param name the name of the server group
     * @return the server group configuration, or <code>null</code> if no server
     *         group named <code>name</code> is configured
     */
    public ServerGroupElement getServerGroup(String name) {
        synchronized (serverGroups) {
            return serverGroups.get(name);
        }
    }

    /**
     * Gets any system properties defined at the domain level. These properties
     * may be extended or overridden by any properties declared at the
     * {@link ServerGroupElement#getSystemProperties() server group level}, the
     * {@link HostModel#getSystemProperties() host level} or the
     * {@link ServerElement#getSystemProperties() server level}.
     *
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<DomainModel> getElementClass() {
        return DomainModel.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        writeNamespaces(streamWriter);

        if (! extensions.isEmpty()) {
            streamWriter.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (String extension : extensions) {
                streamWriter.writeEmptyElement(Element.EXTENSION.getLocalName());
                streamWriter.writeAttribute(Attribute.MODULE.getLocalName(), extension);
            }
            streamWriter.writeEndElement();
        }

        synchronized (profiles) {
            if (! profiles.isEmpty()) {
                streamWriter.writeStartElement(Element.PROFILES.getLocalName());
                for (ProfileElement element : profiles.values()) {
                    streamWriter.writeStartElement(Element.PROFILE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (interfaces) {
            if (! interfaces.isEmpty()) {
                streamWriter.writeStartElement(Element.INTERFACES.getLocalName());
                for (InterfaceElement element : interfaces.values()) {
                    streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (bindingGroups) {
            if (!bindingGroups.isEmpty()) {
                streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUPS.getLocalName());
                for (SocketBindingGroupElement element : bindingGroups.values()) {
                    streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }

        synchronized (deployments) {
            if (! deployments.isEmpty()) {
                streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
                for (DeploymentUnitElement element : deployments.values()) {
                    streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        synchronized (serverGroups) {
            if (! serverGroups.isEmpty()) {
                streamWriter.writeStartElement(Element.SERVER_GROUPS.getLocalName());
                for (ServerGroupElement element : serverGroups.values()) {

                    streamWriter.writeStartElement(Element.SERVER_GROUP.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        // close domain
        streamWriter.writeEndElement();
    }

    boolean addExtension(final String name) {
        return extensions.add(name);
    }

    boolean removeExtension(final String name) {
        return extensions.remove(name);
    }

    SubsystemFactory<?> getSubsystemFactory(final String uri) {
        return subsystemTypes.get(uri);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        subsystemTypesSetter.set(this, new HashMap<String, SubsystemFactory<?>>());
    }
}
