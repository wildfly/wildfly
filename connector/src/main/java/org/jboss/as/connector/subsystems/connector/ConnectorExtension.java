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
package org.jboss.as.connector.subsystems.connector;

import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.connector.subsystems.connector.Constants.BEAN_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.connector.Constants.CONNECTOR;
import static org.jboss.as.connector.subsystems.connector.Constants.DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL;
import static org.jboss.as.connector.subsystems.connector.Constants.DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL;
import static org.jboss.as.connector.subsystems.connector.ConnectorSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.connector.ConnectorSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.connector.ConnectorSubsystemProviders.SUBSYSTEM_REMOVE_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.readBooleanAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectorExtension implements Extension {
    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

    @Override
    public void initialize(final ExtensionContext context) {
        log.debugf("Initializing Connector Extension");
        // Register the connector subsystem
        final SubsystemRegistration registration = context.registerSubsystem(CONNECTOR);

        registration.registerXMLElementWriter(NewConnectorSubsystemParser.INSTANCE);

        // Connector subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, ConnectorSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(REMOVE, ConnectorSubSystemRemove.INSTANCE, SUBSYSTEM_REMOVE_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ConnectorSubsystemDescribeHandler.INSTANCE, ConnectorSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewConnectorSubsystemParser.INSTANCE);
    }

    private static ModelNode createEmptyAddOperation() {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, CONNECTOR);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        return subsystem;
    }

    static final class NewConnectorSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final NewConnectorSubsystemParser INSTANCE = new NewConnectorSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();
            writeArchiveValidation(writer, node);
            writeBeanValidation(writer, node);
            writeDefaultWorkManager(writer, node);
            writer.writeEndElement();
        }

        private void writeArchiveValidation(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            if (hasAnyOf(node, ARCHIVE_VALIDATION_ENABLED, ARCHIVE_VALIDATION_FAIL_ON_ERROR, ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
                writer.writeEmptyElement(Element.ARCHIVE_VALIDATION.getLocalName());
                if (has(node, ARCHIVE_VALIDATION_ENABLED)) {
                    writeAttribute(writer, Attribute.ENABLED, node.require(ARCHIVE_VALIDATION_ENABLED));
                }
                if (has(node, ARCHIVE_VALIDATION_FAIL_ON_ERROR)) {
                    writeAttribute(writer, Attribute.FAIL_ON_ERROR, node.require(ARCHIVE_VALIDATION_FAIL_ON_ERROR));
                }
                if (has(node, ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
                    writeAttribute(writer, Attribute.FAIL_ON_WARN, node.require(ARCHIVE_VALIDATION_FAIL_ON_WARN));
                }
            }
        }

        private void writeBeanValidation(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            if (has(node, BEAN_VALIDATION_ENABLED)) {
                writer.writeEmptyElement(Element.BEAN_VALIDATION.getLocalName());
                writeAttribute(writer, Attribute.ENABLED, node.require(BEAN_VALIDATION_ENABLED));
            }
        }

        private void writeDefaultWorkManager(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            if (hasAnyOf(node, DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL, DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL)) {
                writer.writeEmptyElement(Element.DEFAULT_WORKMANAGER.getLocalName());
                if (has(node, DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL)) {
                    writeAttribute(writer, Attribute.SHORT_RUNNING_THREAD_POOL,
                            node.require(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL));
                }
                if (has(node, DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL)) {
                    writeAttribute(writer, Attribute.LONG_RUNNING_THREAD_POOL,
                            node.require(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL));
                }
            }
        }

        private boolean hasAnyOf(ModelNode node, String... names) {
            for (String current : names) {
                if (has(node, current)) {
                    return true;
                }
            }
            return false;
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
                throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode subsystem = createEmptyAddOperation();
            list.add(subsystem);

            // Handle elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            final EnumSet<Element> requiredElement = EnumSet.of(Element.DEFAULT_WORKMANAGER);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case CONNECTOR_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (!visited.add(element)) {
                            throw unexpectedElement(reader);
                        }

                        switch (element) {
                            case ARCHIVE_VALIDATION: {
                                parseArchiveValidation(reader, subsystem);
                                break;
                            }
                            case BEAN_VALIDATION: {
                                parseBeanValidation(reader, subsystem);
                                break;
                            }
                            case DEFAULT_WORKMANAGER: {
                                parseDefaultWorkManager(reader, subsystem);
                                requiredElement.remove(Element.DEFAULT_WORKMANAGER);
                                break;

                            }
                            default:
                                throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }
            if (!requiredElement.isEmpty()) {
                throw missingRequiredElement(reader, requiredElement);
            }
        }

        private void parseArchiveValidation(final XMLExtendedStreamReader reader, final ModelNode node)
                throws XMLStreamException {

            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLED: {
                        node.get(ARCHIVE_VALIDATION_ENABLED).set(Boolean.parseBoolean(reader.getAttributeValue(i)));
                        break;
                    }
                    case FAIL_ON_ERROR: {
                        node.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR).set(Boolean.parseBoolean(reader.getAttributeValue(i)));

                        break;
                    }
                    case FAIL_ON_WARN: {
                        node.get(ARCHIVE_VALIDATION_FAIL_ON_WARN).set(Boolean.parseBoolean(reader.getAttributeValue(i)));
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
            // Handle elements
            requireNoContent(reader);

        }

        private void parseDefaultWorkManager(final XMLExtendedStreamReader reader, final ModelNode node)
                throws XMLStreamException {

            final EnumSet<Attribute> required = EnumSet.of(Attribute.SHORT_RUNNING_THREAD_POOL,
                    Attribute.LONG_RUNNING_THREAD_POOL);
            final int cnt = reader.getAttributeCount();
            for (int i = 0; i < cnt; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SHORT_RUNNING_THREAD_POOL: {
                        node.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL).set(reader.getAttributeValue(i));
                        required.remove(Attribute.SHORT_RUNNING_THREAD_POOL);
                        break;
                    }
                    case LONG_RUNNING_THREAD_POOL: {
                        node.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL).set(reader.getAttributeValue(i));
                        required.remove(Attribute.LONG_RUNNING_THREAD_POOL);
                        break;
                    }
                }
            }
            if (!required.isEmpty()) {
                missingRequired(reader, required);
            }
            // Handle elements
            requireNoContent(reader);

        }

        private void parseBeanValidation(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
            final boolean enabled = readBooleanAttributeElement(reader, Attribute.ENABLED.getLocalName());
            node.get(BEAN_VALIDATION_ENABLED).set(enabled);
            // Don't add a requireNoContent here as readBooleanAttributeElement already performs that check.
        }
    }


    private static class ConnectorSubsystemDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final ConnectorSubsystemDescribeHandler INSTANCE = new ConnectorSubsystemDescribeHandler();
        @Override
        public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            final ModelNode add = createEmptyAddOperation();
            final ModelNode model = context.getSubModel();

            if (model.hasDefined(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL)) {
                add.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL).set(model.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL));
            }
            if (model.hasDefined(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL)) {
                add.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL).set(model.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL));
            }
            if (model.hasDefined(BEAN_VALIDATION_ENABLED)) {
                add.get(BEAN_VALIDATION_ENABLED).set(model.get(BEAN_VALIDATION_ENABLED));
            }
            if (model.hasDefined(ARCHIVE_VALIDATION_ENABLED)) {
                add.get(ARCHIVE_VALIDATION_ENABLED).set(model.get(ARCHIVE_VALIDATION_ENABLED));
            }
            if (model.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_ERROR)) {
                add.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR).set(model.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR));
            }
            if (model.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
                add.get(ARCHIVE_VALIDATION_FAIL_ON_WARN).set(model.get(ARCHIVE_VALIDATION_FAIL_ON_WARN));
            }

            ModelNode result = new ModelNode();
            result.add(add);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete(new ModelNode());
            return Cancellable.NULL;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
