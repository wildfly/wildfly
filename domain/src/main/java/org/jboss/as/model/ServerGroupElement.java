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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * A server group within a {@link Domain}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupElement extends AbstractModelElement<ServerGroupElement> {

    private static final long serialVersionUID = 3780369374145922407L;

    private final String name;
    private final String profile;
    private final Map<DeploymentUnitKey, ServerGroupDeploymentElement> deploymentMappings = new TreeMap<DeploymentUnitKey, ServerGroupDeploymentElement>();
    private SocketBindingGroupRefElement bindingGroup;
    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param name the name of the server group
     */
    public ServerGroupElement(final Location location, final String name, final String profile) {
        super(location);
        this.name = name;
        this.profile = profile;
    }
    
    public ServerGroupElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        String profile = null;
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
                    case PROFILE: {
                        profile = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (profile == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PROFILE));
        }
        this.name = name;
        this.profile = profile;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case JVM: {
                            // FIXME implement jvm
                            throw new UnsupportedOperationException("implement jvm");
                            //break;
                        }
                        case SOCKET_BINDING_GROUP: {
                            if (bindingGroup != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            bindingGroup = new SocketBindingGroupRefElement(reader);
                            break;
                        }
                        case DEPLOYMENTS: {
                            parseDeployments(reader);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            // FIXME implement system-properties
                            throw new UnsupportedOperationException("implement system-properties");
                            //break;
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
     * Gets the name of the server group.
     * 
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the name of the profile the server group will run.
     * 
     * @return the profile name. Will not be <code>null</code>
     */
    public String getProfile() {
        return profile;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ profile.hashCode() & 0xffffffffL;
        cksum = calculateElementHashOf(deploymentMappings.values(), cksum);
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ServerGroupElement>> target, final ServerGroupElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<ServerGroupElement> getElementClass() {
        return ServerGroupElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.PROFILE.getLocalName(), profile);

        if (bindingGroup != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            bindingGroup.writeContent(streamWriter);
        }
        
        if (! deploymentMappings.isEmpty()) {
            streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
            for (ServerGroupDeploymentElement element : deploymentMappings.values()) {
                streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }
    
    private void parseDeployments(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT: {
                            final ServerGroupDeploymentElement deployment = new ServerGroupDeploymentElement(reader);
                            if (deploymentMappings.containsKey(deployment.getKey())) {
                                throw new XMLStreamException("Deployment " + deployment.getName() + 
                                        " with sha1 hash " + bytesToHexString(deployment.getSha1Hash()) + 
                                        " already declared", reader.getLocation());
                            }
                            deploymentMappings.put(deployment.getKey(), deployment);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                }
                default: throw unexpectedElement(reader);
            }
        }        
    }
}
