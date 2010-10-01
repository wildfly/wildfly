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

import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorElement extends AbstractModelElement<ConnectorElement> {

    private static final long serialVersionUID = 4093084317901337638L;

    /**
     * The parent name for all Remoting connectors, "jboss.remoting.connector".
     */
    public static final ServiceName JBOSS_REMOTING_CONNECTOR = RemotingSubsystemElement.JBOSS_REMOTING.append("connector");

    private final String name;
    private String socketBinding;
    private SaslElement saslElement;
    private String authenticationProvider;
    private Map<String, String> connectorProperties;

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

    /** {@inheritDoc} */
    protected Class<ConnectorElement> getElementClass() {
        return ConnectorElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", name);
        streamWriter.writeAttribute("socket-binding", name);
        if(saslElement != null) {
            streamWriter.writeStartElement(Element.SASL.getLocalName());
            saslElement.writeContent(streamWriter);
        }
        if(authenticationProvider != null) {
            streamWriter.writeStartElement(Element.AUTHENTICATION_PROVIDER.getLocalName());
            streamWriter.writeAttribute("name", authenticationProvider);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
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

    public String getAuthenticationProvider() {
        return authenticationProvider;
    }

    void setAuthenticationProvider(String authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public SaslElement getSaslElement() {
        return saslElement;
    }

    void setSaslElement(SaslElement saslElement) {
        this.saslElement = saslElement;
    }

    public Map<String, String> getConnectorProperties() {
        return connectorProperties;
    }

    void setConnectorProperties(Map<String, String> connectorProperties) {
        this.connectorProperties = connectorProperties;
    }

    static ServiceName connectorName(final String name) {
        return JBOSS_REMOTING_CONNECTOR.append(name);
    }

}
