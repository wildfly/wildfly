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

import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.NAME;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSSubsystemWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    private static final WSSubsystemWriter INSTANCE = new WSSubsystemWriter();

    private WSSubsystemWriter() {
        // forbidden instantiation
    }

    static WSSubsystemWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        // write ws subsystem start element
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode subsystem = context.getModelNode();
        if (has(subsystem, MODIFY_WSDL_ADDRESS)) {
            // write modify-wsdl-address element
            final String modifyWsdlAddress = subsystem.require(MODIFY_WSDL_ADDRESS).asString();
            writeElement(writer, MODIFY_WSDL_ADDRESS, modifyWsdlAddress);
        }
        if (has(subsystem, WSDL_HOST)) {
            // write wsdl-host element
            final String wsdlHost = subsystem.require(WSDL_HOST).asString();
            writeElement(writer, WSDL_HOST, wsdlHost);
        }
        if (has(subsystem, WSDL_PORT)) {
            // write wsdl-port element
            final String wsdlPort = subsystem.require(WSDL_PORT).asString();
            writeElement(writer, WSDL_PORT, wsdlPort);
        }
        if (has(subsystem, WSDL_SECURE_PORT)) {
            // write wsdl-secure-port element
            final String wsdlSecurePort = subsystem.require(WSDL_SECURE_PORT).asString();
            writeElement(writer, WSDL_SECURE_PORT, wsdlSecurePort);
        }
        if (has(subsystem, ENDPOINT_CONFIG)) {
            // write endpoint-config elements
            final ModelNode endpointConfigs = subsystem.get(ENDPOINT_CONFIG);
            writeEndpointConfigs(writer, endpointConfigs);
        }
        // write ws subsystem end element
        writer.writeEndElement();
    }

    private static void writeEndpointConfigs(final XMLExtendedStreamWriter writer, final ModelNode endpointConfigs) throws XMLStreamException {
        ModelNode endpointConfig = null;
        for (final String configName : endpointConfigs.keys()) {
            endpointConfig = endpointConfigs.get(configName);
            // start endpoint-config element
            writer.writeStartElement(Constants.ENDPOINT_CONFIG);
            writer.writeAttribute(Constants.NAME, configName);
            // write pre-handler-chain elements
            if (endpointConfig.hasDefined(Constants.PRE_HANDLER_CHAIN)) {
                final ModelNode handlerChains = endpointConfig.get(Constants.PRE_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, true);
            }
            // write post-handler-chain elements
            if (endpointConfig.hasDefined(Constants.POST_HANDLER_CHAIN)) {
                final ModelNode handlerChains = endpointConfig.get(Constants.POST_HANDLER_CHAIN);
                writeHandlerChains(writer, handlerChains, false);
            }
            // write property elements
            if (endpointConfig.hasDefined(Constants.PROPERTY)) {
                final ModelNode properties = endpointConfig.get(PROPERTY);
                writeProperties(writer, properties);
            }
            // close endpoint-config element
            writer.writeEndElement();
        }
    }

    private static void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode properties) throws XMLStreamException {
        ModelNode property = null;
        String propertyValue = null;
        // write property elements
        for (final String propertyName : properties.keys()) {
            property = properties.get(propertyName);
            writer.writeStartElement(PROPERTY);
            writer.writeAttribute(NAME, propertyName);
            if (property.hasDefined(VALUE)) {
                propertyValue = property.get(VALUE).asString();
                writer.writeAttribute(VALUE, propertyValue);
            }
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
                    writer.writeAttribute(CLASS, handler.get(CLASS).asString());
                    writer.writeEndElement();
                }
            }
            // end either pre-handler-chain or post-handler-chain element
            writer.writeEndElement();
        }
    }

    private static void writeElement(final XMLExtendedStreamWriter writer, final String elementName, final String elementValue) throws XMLStreamException {
        writer.writeStartElement(elementName);
        writer.writeCharacters(elementValue);
        writer.writeEndElement();
    }

    private static boolean has(final ModelNode node, final String name) {
        return node.has(name) && node.get(name).isDefined();
    }

}
