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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jmx.CommonAttributes.JMX;
import static org.jboss.as.jmx.CommonAttributes.REMOTING_CONNECTOR;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
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
    private static final String RESOURCE_NAME = JMXExtension.class.getPackage().getName() + ".LocalDescriptions";


    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, JMXExtension.class.getClassLoader(), true, false);
    }

    static final JMXSubsystemParser_1_2 parserCurrent = new JMXSubsystemParser_1_2();
    static final JMXSubsystemParser_1_1 parser11 = new JMXSubsystemParser_1_1();
    static final JMXSubsystemParser_1_0 parser10 = new JMXSubsystemParser_1_0();
    static final JMXSubsystemWriter writer = new JMXSubsystemWriter();

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        registration.registerSubsystemModel(new JMXSubsystemRootResource());
        registration.registerXMLElementWriter(writer);

        registerTransformers1_0_0(registration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_0.getUriString(), parser10);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_1.getUriString(), parser11);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_2.getUriString(), parserCurrent);
    }

    private static ModelNode createAddOperation() {
        return createOperation(ADD);
    }

    private static ModelNode createOperation(String name, String...addressElements) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(name);
        op.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        for (int i = 0 ; i < addressElements.length ; i++) {
            op.get(OP_ADDR).add(addressElements[i], addressElements[++i]);
        }
        return op;
    }

    private void registerTransformers1_0_0(SubsystemRegistration registration) {
        // Register the transformers
        final TransformersSubRegistration transformers = registration.registerModelTransformers(ModelVersion.create(1, 0, 0), new AbstractSubsystemTransformer(SUBSYSTEM_NAME) {
            @Override
            protected ModelNode transformModel(TransformationContext context, ModelNode model) {
                boolean showModel = model.get(CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED).isDefined();
                ModelNode result = model.clone();
                result.get(CommonAttributes.SHOW_MODEL).set(showModel);
                result.remove(CommonAttributes.EXPOSE_MODEL);
                return result;
            }
        });


        TransformersSubRegistration expressions = transformers.registerSubResource(ExposeModelResourceExpression.INSTANCE.getPathElement());

        expressions.discardOperations(ADD, REMOVE, WRITE_ATTRIBUTE_OPERATION, READ_ATTRIBUTE_OPERATION);

        TransformersSubRegistration resolved = transformers.registerSubResource(ExposeModelResourceResolved.INSTANCE.getPathElement());
        resolved.discardOperations(WRITE_ATTRIBUTE_OPERATION);
        resolved.registerOperationTransformer(ADD, new AbstractOperationTransformer() {
            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                ModelNode node = new ModelNode();
                node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                node.get(OP_ADDR).set(address.subAddress(0, address.size() - 1).toModelNode());
                node.get(NAME).set(CommonAttributes.SHOW_MODEL);
                node.get(VALUE).set(true);
                return node;
            }
        });

        resolved.registerOperationTransformer(REMOVE, new AbstractOperationTransformer() {
            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                ModelNode node = new ModelNode();
                node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                node.get(OP_ADDR).set(address.subAddress(0, address.size() - 1).toModelNode());
                node.get(NAME).set(CommonAttributes.SHOW_MODEL);
                node.get(VALUE).set(false);
                return node;
            }
        });
        resolved.registerOperationTransformer(READ_ATTRIBUTE_OPERATION, new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) {
                return new TransformedOperation(null, new OperationResultTransformer() {
                        @Override
                        public ModelNode transformResult(ModelNode result) {
                            if (operation.get(NAME).asString().equals(CommonAttributes.DOMAIN_NAME)) {
                                result.get(RESULT).set(CommonAttributes.DEFAULT_RESOLVED_DOMAIN);
                            }
                            result.get(OUTCOME).set(SUCCESS);
                            result.get(RESULT);
                            return result;
                        }
                });
            }
        });
    }

    private static class JMXSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            list.add(createAddOperation());

            boolean gotConnector = false;

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case JMX_CONNECTOR: {
                        if (gotConnector) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.JMX_CONNECTOR.getLocalName());
                        }
                        parseConnector(reader);
                        gotConnector = true;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        void parseConnector(XMLExtendedStreamReader reader) throws XMLStreamException {
            JmxLogger.ROOT_LOGGER.jmxConnectorNotSupported();
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
                    }
                    case REGISTRY_BINDING: {
                        registryBinding = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (serverBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SERVER_BINDING));
            }
            if (registryBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REGISTRY_BINDING));
            }
        }
    }

    private static class JMXSubsystemParser_1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            boolean showModel = false;

            ParseUtils.requireNoAttributes(reader);

            ModelNode connectorAdd = null;
            list.add(createAddOperation());
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case SHOW_MODEL:
                        if (showModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.SHOW_MODEL.getLocalName());
                        }
                        if (parseShowModelElement(reader)) {
                            //Add the show-model=>resolved part with the default domain name
                            ModelNode op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
                            //Use false here to keep total backwards compatibility
                            op.get(CommonAttributes.PROPER_PROPERTY_FORMAT).set(false);
                            list.add(op);
                        }
                        showModel = true;
                        break;
                    case REMOTING_CONNECTOR: {
                        if (connectorAdd != null) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.REMOTING_CONNECTOR.getLocalName());
                        }
                        list.add(parseRemoteConnector(reader));
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        protected ModelNode parseRemoteConnector(final XMLExtendedStreamReader reader) throws XMLStreamException {

            final ModelNode connector = new ModelNode();
            connector.get(OP).set(ADD);
            connector.get(OP_ADDR).add(SUBSYSTEM, JMX).add(REMOTING_CONNECTOR, CommonAttributes.JMX);

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case USE_MANAGEMENT_ENDPOINT: {
                        RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.parseAndSetParameter(value, connector, reader);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            ParseUtils.requireNoContent(reader);
            return connector;
        }


        private boolean parseShowModelElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            ParseUtils.requireSingleAttribute(reader, CommonAttributes.VALUE);
            return ParseUtils.readBooleanAttributeElement(reader, CommonAttributes.VALUE);
        }
    }

    private static class JMXSubsystemParser_1_2 extends JMXSubsystemParser_1_1 {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            boolean showResolvedModel = false;
            boolean showExpressionModel = false;
            boolean connectorAdd = false;

            ParseUtils.requireNoAttributes(reader);

            list.add(createAddOperation());

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case EXPOSE_RESOLVED_MODEL:
                        if (showResolvedModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_RESOLVED_MODEL.getLocalName());
                        }
                        showResolvedModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.RESOLVED));
                        break;
                    case EXPOSE_EXPRESSION_MODEL:
                        if (showExpressionModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_EXPRESSION_MODEL.getLocalName());
                        }
                        showExpressionModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.EXPRESSION));
                        break;
                    case REMOTING_CONNECTOR: {
                        if (connectorAdd) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.REMOTING_CONNECTOR.getLocalName());
                        }
                        connectorAdd = true;
                        list.add(parseRemoteConnector(reader));
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        private ModelNode parseShowModelElement(XMLExtendedStreamReader reader, String showModelChild) throws XMLStreamException {

            ModelNode op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, showModelChild);

            String domainName = null;
            Boolean properPropertyFormat = null;

            for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case DOMAIN_NAME:
                    ExposeModelResource.getDomainNameAttribute(showModelChild).parseAndSetParameter(value, op, reader);
                    break;
                case PROPER_PROPETY_FORMAT:
                    if (showModelChild.equals(CommonAttributes.RESOLVED)) {
                        ExposeModelResourceResolved.PROPER_PROPERTY_FORMAT.parseAndSetParameter(value, op, reader);
                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            if (domainName == null && properPropertyFormat == null) {
                ParseUtils.requireNoContent(reader);
            }
            return op;
        }
    }

    private static class JMXSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            Namespace schemaVer = Namespace.CURRENT;
            ModelNode node = context.getModelNode();

            context.startSubsystemElement(schemaVer.getUriString(), false);
            if (node.hasDefined(CommonAttributes.EXPOSE_MODEL)) {
                ModelNode showModel = node.get(CommonAttributes.EXPOSE_MODEL);
                if (showModel.hasDefined(CommonAttributes.RESOLVED)) {
                    writer.writeEmptyElement(Element.EXPOSE_RESOLVED_MODEL.getLocalName());
                    ExposeModelResourceResolved.DOMAIN_NAME.marshallAsAttribute(showModel.get(CommonAttributes.RESOLVED), false, writer);
                    ExposeModelResourceResolved.PROPER_PROPERTY_FORMAT.marshallAsAttribute(showModel.get(CommonAttributes.RESOLVED), false, writer);
                }
                if (showModel.hasDefined(CommonAttributes.EXPRESSION)) {
                    writer.writeEmptyElement(Element.EXPOSE_EXPRESSION_MODEL.getLocalName());
                    ExposeModelResourceExpression.DOMAIN_NAME.marshallAsAttribute(showModel.get(CommonAttributes.EXPRESSION), false, writer);
                }
            }
            if (node.hasDefined(CommonAttributes.REMOTING_CONNECTOR)) {
                writer.writeStartElement(Element.REMOTING_CONNECTOR.getLocalName());
                final ModelNode resourceModel = node.get(CommonAttributes.REMOTING_CONNECTOR).get(CommonAttributes.JMX);
                RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.marshallAsAttribute(resourceModel, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

}
