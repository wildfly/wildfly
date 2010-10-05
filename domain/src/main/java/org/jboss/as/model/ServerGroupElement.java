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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A server group within a {@link DomainModel}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupElement extends AbstractModelElement<ServerGroupElement> {

    private static final long serialVersionUID = 3780369374145922407L;

    private final String name;
    private final String profile;
    private final NavigableMap<String, ServerGroupDeploymentElement> deploymentMappings = new TreeMap<String, ServerGroupDeploymentElement>();
    private SocketBindingGroupRefElement bindingGroup;
    private JvmElement jvm;
    private PropertiesElement systemProperties;

    /**
     * Construct a new instance.
     *
     * @param name the name of the server group
     */
    public ServerGroupElement(final String name, final String profile) {
        this.name = name;
        this.profile = profile;
    }

    /**
     * Gets the name of the server group.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of the profile that servers in the server group will run.
     *
     * @return the profile name. Will not be <code>null</code>
     */
    public String getProfileName() {
        return profile;
    }

    /**
     * Gets the default jvm configuration for servers in this group. Which jvm to
     * use can be overridden at the {@link ServerElement#getJvm() server level}.
     * The details of the configuration of this jvm can be overridden at the
     * @{link {@link HostModel#getJvm(String) host level} or at the
     * {@link ServerElement#getJvm() server level}.
     *
     * @return the jvm configuration, or <code>null</code> if there is none
     */
    public JvmElement getJvm() {
        return jvm;
    }

    /**
     * Sets the default jvm configuration for servers in this group.
     *
     * param jvm the jvm configuration. May be <code>null</code>
     */
    void setJvm(JvmElement jvm) {
        this.jvm = jvm;
    }

    /**
     * Gets the default
     * {@link DomainModel#getSocketBindingGroup(String) domain-level socket binding group}
     * assignment for this server group.
     *
     * @return the socket binding group reference, or <code>null</code>
     */
    public SocketBindingGroupRefElement getSocketBindingGroup() {
        return bindingGroup;
    }

    /**
     * Sets the default
     * {@link DomainModel#getSocketBindingGroup(String) domain-level socket binding group}
     * assignment for this server group.
     *
     * param ref the socket binding group reference, or <code>null</code>
     */
    void setSocketBindingGroupRefElement(SocketBindingGroupRefElement ref) {
        this.bindingGroup = ref;
    }

    /**
     * Gets the deployments mapped to this server group.
     *
     * @return the deployments. May be empty but will not be <code>null</code>
     */
    public Set<ServerGroupDeploymentElement> getDeployments() {
        Set<ServerGroupDeploymentElement> deps = new LinkedHashSet<ServerGroupDeploymentElement>();
        synchronized (deploymentMappings) {
            for (Map.Entry<String, ServerGroupDeploymentElement> entry : deploymentMappings.entrySet()) {
                deps.add(entry.getValue());
            }
        }
        return deps;
    }

    /**
     * Gets any system properties defined at the server group level for this
     * server group. These properties can extend and override any properties
     * declared at the {@link DomainModel#getSystemProperties() domain level} and
     * may in turn be extended or overridden by any properties declared at the
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
    protected Class<ServerGroupElement> getElementClass() {
        return ServerGroupElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.PROFILE.getLocalName(), profile);


        if (jvm != null) {
            streamWriter.writeStartElement(Element.JVM.getLocalName());
            jvm.writeContent(streamWriter);
        }

        if (bindingGroup != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            bindingGroup.writeContent(streamWriter);
        }

        synchronized (deploymentMappings) {
            if (! deploymentMappings.isEmpty()) {
                streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
                for (ServerGroupDeploymentElement element : deploymentMappings.values()) {
                    streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }

        streamWriter.writeEndElement();
    }

    private void parseDeployments(XMLExtendedStreamReader reader) throws XMLStreamException {
        // FIXME replace with SimpleRefResolver
        final RefResolver<String, DeploymentRepositoryElement> resolver = new RefResolver<String, DeploymentRepositoryElement>() {
            private static final long serialVersionUID = 1L;
            /** Always returns <code>null</code> since full domain does not support deployment-repository */
            @Override
            public DeploymentRepositoryElement resolveRef(String ref) {
                return null;
            }
        };

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT: {
                            final ServerGroupDeploymentElement deployment = new ServerGroupDeploymentElement(reader, resolver);
                            if (deploymentMappings.containsKey(deployment.getUniqueName())) {
                                throw new XMLStreamException("Deployment " + deployment.getUniqueName() +
                                        " with sha1 hash " + bytesToHexString(deployment.getSha1Hash()) +
                                        " already declared", reader.getLocation());
                            }
                            deploymentMappings.put(deployment.getUniqueName(), deployment);
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
