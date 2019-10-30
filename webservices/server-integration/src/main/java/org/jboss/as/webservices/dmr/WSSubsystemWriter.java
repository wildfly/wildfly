/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.NAME;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class WSSubsystemWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    WSSubsystemWriter() {
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        // write ws subsystem start element
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode subsystem = context.getModelNode();

        if (Attributes.STATISTICS_ENABLED.isMarshallable(subsystem)) {
            Attributes.STATISTICS_ENABLED.marshallAsAttribute(subsystem, writer);
        }

        for (SimpleAttributeDefinition attr : Attributes.SUBSYSTEM_ATTRIBUTES) {
            attr.marshallAsElement(subsystem, true, writer);
        }
        if (subsystem.hasDefined(ENDPOINT_CONFIG)) {
            // write endpoint-config elements
            final ModelNode endpointConfigs = subsystem.get(ENDPOINT_CONFIG);
            writeConfigs(ENDPOINT_CONFIG, writer, endpointConfigs);
        }
        if (subsystem.hasDefined(CLIENT_CONFIG)) {
            // write client-config elements
            final ModelNode clientConfigs = subsystem.get(CLIENT_CONFIG);
            writeConfigs(CLIENT_CONFIG, writer, clientConfigs);
        }
        // write ws subsystem end element
        writer.writeEndElement();
    }

    private static void writeConfigs(final String elementName, final XMLExtendedStreamWriter writer, final ModelNode configs) throws XMLStreamException {
        ModelNode config = null;
        for (final String configName : configs.keys()) {
            config = configs.get(configName);
            // start config element
            writer.writeStartElement(elementName);
            writer.writeAttribute(Constants.NAME, configName);
            // write pre-handler-chain elements
            if (config.hasDefined(Constants.PRE_HANDLER_CHAIN)) {
                final ModelNode handlerChains = config.get(Constants.PRE_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, true);
            }
            // write post-handler-chain elements
            if (config.hasDefined(Constants.POST_HANDLER_CHAIN)) {
                final ModelNode handlerChains = config.get(Constants.POST_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, false);
            }
            // write property elements
            if (config.hasDefined(Constants.PROPERTY)) {
                final ModelNode properties = config.get(PROPERTY);
                writeProperties(writer, properties);
            }
            // close endpoint-config element
            writer.writeEndElement();
        }
    }

    private static void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode properties) throws XMLStreamException {
        ModelNode property;
        // write property elements
        for (final String propertyName : properties.keys()) {
            property = properties.get(propertyName);
            writer.writeStartElement(PROPERTY);
            writer.writeAttribute(NAME, propertyName);
            Attributes.VALUE.marshallAsAttribute(property, false, writer);
            writer.writeEndElement();
        }
    }

    private static void writeHandlerChains(final XMLExtendedStreamWriter writer, final ModelNode handlerChains, final boolean isPre) throws XMLStreamException {
        ModelNode handlerChain = null;
        ModelNode handler = null;
        for (final String handlerChainName : handlerChains.keys()) {
            handlerChain = handlerChains.get(handlerChainName);
            // start either pre-handler-chain or post-handler-chain element
            writer.writeStartElement(isPre ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN);
            writer.writeAttribute(NAME, handlerChainName);
            if (handlerChain.hasDefined(PROTOCOL_BINDINGS)) {
                final String protocolBinding = handlerChain.get(PROTOCOL_BINDINGS).asString();
                writer.writeAttribute(PROTOCOL_BINDINGS, protocolBinding);
            }
            // write handler elements
            if (handlerChain.hasDefined(HANDLER)) {
                for (final String handlerName : handlerChain.require(HANDLER).keys()) {
                    handler = handlerChain.get(HANDLER).get(handlerName);
                    writer.writeStartElement(HANDLER);
                    writer.writeAttribute(NAME, handlerName);
                    Attributes.CLASS.marshallAsAttribute(handler, writer);
                    writer.writeEndElement();
                }
            }
            // end either pre-handler-chain or post-handler-chain element
            writer.writeEndElement();
        }
    }
}
