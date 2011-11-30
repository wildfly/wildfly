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
 * @author Kabir Khan
 */
public class JMXExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jmx";
    private static final String INVOKE_MBEAN_RAW = "invoke-mbean-raw";
    private static final String GET_MBEAN_INFO_RAW = "get-mbean-info-raw";

    static final JMXSubsystemParser_1_1 parserCurrent = new JMXSubsystemParser_1_1();
    static final JMXSubsystemParser_1_0 parser10 = new JMXSubsystemParser_1_0();


    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JMXSubsystemProviders.SUBSYSTEM);
        // Subsystem operation handlers
        registration.registerOperationHandler(ADD, JMXSubsystemAdd.INSTANCE, JMXSubsystemProviders.SUBSYTEM_ADD, false);
        registration.registerOperationHandler(INVOKE_MBEAN_RAW, new InvokeMBeanRaw(), JMXSubsystemProviders.INVOKE_MBEAN_RAW, false);
        registration.registerOperationHandler(GET_MBEAN_INFO_RAW, new GetMBeanInfoRaw(), JMXSubsystemProviders.GET_MBEAN_INFO_RAW, false);
        registration.registerOperationHandler(DESCRIBE, JMXDescribeHandler.INSTANCE, JMXDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(JMXConnectorAdd.OPERATION_NAME, JMXConnectorAdd.INSTANCE, JMXSubsystemProviders.JMX_CONNECTOR_ADD, false);
        registration.registerOperationHandler(JMXConnectorRemove.OPERATION_NAME, JMXConnectorRemove.INSTANCE, JMXSubsystemProviders.JMX_CONNECTOR_REMOVE, false);

        subsystem.registerXMLElementWriter(parserCurrent);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.JMX_1_0.getUriString(), parser10);
        context.setSubsystemXmlMapping(Namespace.JMX_1_1.getUriString(), parserCurrent);
    }

    private static ModelNode createAddOperation(Boolean showModel) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        if (showModel != null) {
            subsystem.get(CommonAttributes.SHOW_MODEL).set(showModel.booleanValue());
        }
        return subsystem;
    }

    private static ModelNode createAddConnectorOperation(String serverBinding, String registryBinding) {
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(JMXConnectorAdd.OPERATION_NAME);
        connector.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        connector.get(CommonAttributes.SERVER_BINDING).set(serverBinding);
        connector.get(CommonAttributes.REGISTRY_BINDING).set(registryBinding);
        return connector;
    }

    private static class JMXSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            list.add(createAddOperation(null));

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
            list.add(createAddConnectorOperation(serverBinding, registryBinding));
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            Namespace schemaVer = Namespace.CURRENT;
            ModelNode node = context.getModelNode();

            context.startSubsystemElement(schemaVer.getUriString(), false);
            if (node.hasDefined(CommonAttributes.SERVER_BINDING)) {
                writer.writeStartElement(Element.JMX_CONNECTOR.getLocalName());
                writer.writeAttribute(Attribute.REGISTRY_BINDING.getLocalName(), node.get(CommonAttributes.REGISTRY_BINDING).asString());
                writer.writeAttribute(Attribute.SERVER_BINDING.getLocalName(), node.get(CommonAttributes.SERVER_BINDING).asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private static class JMXSubsystemParser_1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            Boolean showModel = null;

            ParseUtils.requireNoAttributes(reader);

            ModelNode connectorAdd = null;
            while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch(element){
                    case SHOW_MODEL:
                        if (showModel != null) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.SHOW_MODEL.getLocalName());
                        }
                        showModel = parseShowModelElement(reader);
                        break;
                    case JMX_CONNECTOR: {
                        if (connectorAdd != null) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.JMX_CONNECTOR.getLocalName());
                        }
                        connectorAdd = parseConnector(reader);
                        break;
                    } default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            list.add(createAddOperation(showModel));
            if (connectorAdd != null) {
                list.add(connectorAdd);
            }
        }


        boolean parseShowModelElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            ParseUtils.requireSingleAttribute(reader, CommonAttributes.VALUE);
            return ParseUtils.readBooleanAttributeElement(reader, CommonAttributes.VALUE);
        }

        ModelNode parseConnector(XMLExtendedStreamReader reader) throws XMLStreamException {
            String serverBinding = null;
            String registryBinding = null;
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
            return createAddConnectorOperation(serverBinding, registryBinding);
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            Namespace schemaVer = Namespace.CURRENT;
            ModelNode node = context.getModelNode();

            context.startSubsystemElement(schemaVer.getUriString(), false);
            if (node.hasDefined(CommonAttributes.SHOW_MODEL)) {
                writer.writeEmptyElement(Element.SHOW_MODEL.getLocalName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), node.get(CommonAttributes.SHOW_MODEL).asString());
            }
            if (node.hasDefined(CommonAttributes.SERVER_BINDING)) {
                writer.writeStartElement(Element.JMX_CONNECTOR.getLocalName());
                writer.writeAttribute(Attribute.REGISTRY_BINDING.getLocalName(), node.get(CommonAttributes.REGISTRY_BINDING).asString());
                writer.writeAttribute(Attribute.SERVER_BINDING.getLocalName(), node.get(CommonAttributes.SERVER_BINDING).asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private static class JMXDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final JMXDescribeHandler INSTANCE = new JMXDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);
            context.getResult().add(createAddOperation(model.hasDefined(CommonAttributes.SHOW_MODEL) ? model.get(CommonAttributes.SHOW_MODEL).asBoolean() : null));
            if (model.hasDefined(CommonAttributes.SERVER_BINDING)) {
                context.getResult().add(createAddConnectorOperation(model.require(CommonAttributes.SERVER_BINDING).asString(), model.require(CommonAttributes.REGISTRY_BINDING).asString()));
            }
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }

    }
}
