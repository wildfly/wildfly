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

package org.jboss.as.remoting;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.ChannelListener;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.channels.ConnectedStreamChannel;

import javax.xml.stream.XMLStreamException;
import java.net.InetSocketAddress;
import java.util.EnumSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorElement extends AbstractModelElement<ConnectorElement> implements ServiceActivator {

    private static final long serialVersionUID = 4093084317901337638L;

    /**
     * The parent name for all Remoting connectors, "jboss.remoting.connector".
     */
    public static final ServiceName JBOSS_REMOTING_CONNECTOR = RemotingSubsystemElement.JBOSS_REMOTING.append("connector");

    private final String name;
    private String socketBinding;
    private SaslElement saslElement;
    private String authenticationProvider;
    private PropertiesElement connectorProperties;

    public ConnectorElement(final String name, final String socketBinding) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        if (socketBinding == null) {
            throw new IllegalArgumentException("socketBinding is null");
        }
        this.socketBinding = socketBinding;
    }

    public ConnectorElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String name = null;
        String socketBinding = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.SOCKET_BINDING);
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
                    case SOCKET_BINDING: {
                        socketBinding = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert name != null;
        assert socketBinding != null;
        this.name = name;
        this.socketBinding = socketBinding;
        // Handle nested elements.
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        throw unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case SASL: {
                            saslElement = new SaslElement(reader);
                            break;
                        }
                        case PROPERTIES: {
                            connectorProperties = new PropertiesElement(reader);
                            break;
                        }
                        case AUTHENTICATION_PROVIDER: {
                            authenticationProvider = readStringAttributeElement(reader, "name");
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

    /** {@inheritDoc} */
    public long elementHash() {
        return name.hashCode() & 0xFFFFFFFF;
    }

    /** {@inheritDoc} */
    protected Class<ConnectorElement> getElementClass() {
        return ConnectorElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", name);
        streamWriter.writeAttribute("socket-binding", name);
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    public ConnectorElement clone() {
        return super.clone();
    }

    /**
     * Get the name of this connector.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the socket binding name for this element.
     *
     * @return the socket binding name
     */
    public String getSocketBinding() {
        return socketBinding;
    }

    /**
     * Install this connector.
     *
     * @param context the service activator context
     */
    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        final OptionMap.Builder builder = OptionMap.builder();

        // First, apply options to option map.
        if (saslElement != null) saslElement.applyTo(builder);
        // todo: apply connector properties to option map

        // Create the service.
        final ConnectorService connectorService = new ConnectorService();
        connectorService.setOptionMap(builder.getMap());

        // Register the service with the container and inject dependencies.
        final ServiceName connectorName = JBOSS_REMOTING_CONNECTOR.append(name);
        final BatchServiceBuilder<ChannelListener<ConnectedStreamChannel<InetSocketAddress>>> serviceBuilder = batchBuilder.addService(connectorName, connectorService);
        serviceBuilder.addDependency(connectorName.append("auth-provider"), ServerAuthenticationProvider.class, connectorService.getAuthenticationProviderInjector());
        serviceBuilder.addDependency(RemotingSubsystemElement.JBOSS_REMOTING_ENDPOINT, Endpoint.class, connectorService.getEndpointInjector());
        serviceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);

        // todo: create XNIO connector service from socket-binding, with dependency on connectorName
    }
}
