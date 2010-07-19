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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.EnumSet;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.PropertiesElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.ChannelListener;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.channels.ConnectedStreamChannel;

import javax.xml.stream.XMLStreamException;

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
    private AbstractAuthenticationProviderElement<?> authenticationProvider;
    private PropertiesElement connectorProperties;

    public ConnectorElement(final Location location, final String name, final String socketBinding) {
        super(location);
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
        super(reader);
        // Handle attributes
        String name = null;
        String socketBinding = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.SOCKET_BINDING);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                reader.handleAttribute(this, i);
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
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    // should mean we're done, so ignore it.
                    break;
                }
                case START_ELEMENT: {
                    if (RemotingSubsystemElement.NAMESPACES.contains(reader.getNamespaceURI())) {
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
                            // todo: auth provider, properties.
                            default: throw unexpectedElement(reader);
                        }
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw new IllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return name.hashCode() & 0xFFFFFFFF;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ConnectorElement>> target, final ConnectorElement other) {
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
     * @param container the container
     * @param batchBuilder the current batch builder
     */
    public void activate(final ServiceContainer container, final BatchBuilder batchBuilder) {
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
        serviceBuilder.addDependency(connectorName.append("auth-provider")).toInjector(connectorService.getAuthenticationProviderInjector());
        serviceBuilder.addDependency(RemotingSubsystemElement.JBOSS_REMOTING_ENDPOINT).toInjector(connectorService.getEndpointInjector());
        serviceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);

        // todo: create XNIO connector service from socket-binding, with dependency on connectorName
    }
}
