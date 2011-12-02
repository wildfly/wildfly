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

package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the JMX subsystem.
 *
 * @author Emanuel Muckenhuber
 */
public class JMXExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jmx";

    static final JMXSubsystemParser parsers = new JMXSubsystemParser();

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JMXSubsystemProviders.SUBSYSTEM);
        // Subsystem operation handlers
        registration.registerOperationHandler(ADD, JMXSubsystemAdd.INSTANCE, JMXSubsystemProviders.SUBSYTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, JMXDescribeHandler.INSTANCE, JMXDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(JMXConnectorAdd.OPERATION_NAME, JMXConnectorAdd.INSTANCE, JMXSubsystemProviders.JMX_CONNECTOR_ADD, false);
        registration.registerOperationHandler(JMXConnectorRemove.OPERATION_NAME, JMXConnectorRemove.INSTANCE, JMXSubsystemProviders.JMX_CONNECTOR_REMOVE, false);

        subsystem.registerXMLElementWriter(parsers);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.JMX_1_0.getUriString(), parsers);
        context.setSubsystemXmlMapping(Namespace.JMX_1_1.getUriString(), parsers);
    }

    private static ModelNode createAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    private static ModelNode createAddConnectorOperation(String serverBinding, String registryBinding, String passwordFile, String accessFile) {
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(JMXConnectorAdd.OPERATION_NAME);
        connector.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        connector.get(CommonAttributes.SERVER_BINDING).set(serverBinding);
        connector.get(CommonAttributes.REGISTRY_BINDING).set(registryBinding);
        if(passwordFile != null){
            connector.get(CommonAttributes.PASSWORD_FILE).set(passwordFile);
        }
        if(accessFile != null){
            connector.get(CommonAttributes.ACCESS_FILE).set(accessFile);
        }

        return connector;
    }

    static class JMXSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        private volatile Namespace schemaVer;

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            schemaVer = Namespace.forUri(reader.getNamespaceURI());

            list.add(createAddOperation());
            ParseUtils.requireNoAttributes(reader);
            boolean gotConnector = false;

            while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch(element){
                    case JMX_CONNECTOR: {
                        if (gotConnector) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.JMX_CONNECTOR.getLocalName());
                        }
                        parseConnector(reader, list);
                        gotConnector = true;
                        break;
                    } default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        void parseConnector(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            String serverBinding = null;
            String registryBinding = null;
            String passwordFile = null;
            String accessFile = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SERVER_BINDING: {
                        serverBinding = value;
                        break;
                    } case REGISTRY_BINDING: {
                        registryBinding = value;
                        break;
                    } case PASSWORD_FILE: {
                        if(schemaVer == Namespace.JMX_1_0) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        passwordFile = value;
                        break;
                    } case ACCESS_FILE: {
                        if(schemaVer == Namespace.JMX_1_0) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        accessFile = value;
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if(serverBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SERVER_BINDING));
            }
            if(registryBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REGISTRY_BINDING));
            }
            list.add(createAddConnectorOperation(serverBinding, registryBinding, passwordFile, accessFile));
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            Namespace schemaVer = this.schemaVer == null ? Namespace.CURRENT : this.schemaVer;
            ModelNode node = context.getModelNode();

            if(!node.has(CommonAttributes.SERVER_BINDING)) {
                throw new XMLStreamException("Required attribute \"server-binding\" is not defined");
            }
            if(!node.has(CommonAttributes.REGISTRY_BINDING)) {
                throw new XMLStreamException("Required attribute \"registry-binding\" is not defined");
            }

            if ((node.hasDefined(CommonAttributes.PASSWORD_FILE) && !node.hasDefined(CommonAttributes.ACCESS_FILE)) ||
                    (!node.hasDefined(CommonAttributes.PASSWORD_FILE) && node.hasDefined(CommonAttributes.ACCESS_FILE))) {
                throw new XMLStreamException("Both \"password-file\" and \"access-file\" attributes must be provided");
            }

            if (node.hasDefined(CommonAttributes.PASSWORD_FILE) && (schemaVer == Namespace.UNKNOWN ||schemaVer == Namespace.JMX_1_0)) {
                schemaVer = Namespace.JMX_1_1;
            }

            context.startSubsystemElement(schemaVer.getUriString(), false);

            if(node.has(CommonAttributes.SERVER_BINDING)) {
                writer.writeStartElement(Element.JMX_CONNECTOR.getLocalName());
                writer.writeAttribute(Attribute.SERVER_BINDING.getLocalName(), node.get(CommonAttributes.SERVER_BINDING).asString());
                writer.writeAttribute(Attribute.REGISTRY_BINDING.getLocalName(), node.get(CommonAttributes.REGISTRY_BINDING).asString());
                if (node.hasDefined(CommonAttributes.PASSWORD_FILE)) {
                    writer.writeAttribute(Attribute.PASSWORD_FILE.getLocalName(), node.get(CommonAttributes.PASSWORD_FILE).asString());
                }
                if (node.hasDefined(CommonAttributes.ACCESS_FILE)) {
                    writer.writeAttribute(Attribute.ACCESS_FILE.getLocalName(), node.get(CommonAttributes.ACCESS_FILE).asString());
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private static class JMXDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final JMXDescribeHandler INSTANCE = new JMXDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);
            context.getResult().add(createAddOperation());
            context.getResult().add(createAddConnectorOperation(model.require(CommonAttributes.SERVER_BINDING).asString(), model.require(CommonAttributes.REGISTRY_BINDING).asString(), model.hasDefined(CommonAttributes.PASSWORD_FILE) ? model.get(CommonAttributes.PASSWORD_FILE).asString() : null, model.hasDefined(CommonAttributes.ACCESS_FILE) ? model.get(CommonAttributes.ACCESS_FILE).asString() : null));
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }

    }
}
