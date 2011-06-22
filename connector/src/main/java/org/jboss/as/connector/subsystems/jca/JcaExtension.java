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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_DEBUG;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.JCA;
import static org.jboss.as.connector.subsystems.jca.Constants.LONG_RUNNING_THREADS;
import static org.jboss.as.connector.subsystems.jca.Constants.SHORT_RUNNING_THREADS;
import static org.jboss.as.connector.subsystems.jca.Constants.THREAD_POOL;
import static org.jboss.as.connector.subsystems.jca.JcaSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.jca.JcaSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.jca.JcaSubsystemProviders.SUBSYSTEM_REMOVE_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addBoundedQueueThreadPool;
import static org.jboss.as.threads.ThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.threads.BoundedQueueThreadPoolAdd;
import org.jboss.as.threads.BoundedQueueThreadPoolRemove;
import org.jboss.as.threads.ThreadsParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JcaExtension implements Extension {
    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

    private static String SUBSYSTEM_NAME = "jca";

    @Override
    public void initialize(final ExtensionContext context) {
        log.debugf("Initializing Connector Extension");
        // Register the connector subsystem
        final SubsystemRegistration registration = context.registerSubsystem(JCA);

        registration.registerXMLElementWriter(NewConnectorSubsystemParser.INSTANCE);

        // Connector subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, JcaSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(REMOVE, JcaSubSystemRemove.INSTANCE, SUBSYSTEM_REMOVE_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ConnectorSubsystemDescribeHandler.INSTANCE,
                ConnectorSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);


        final ManagementResourceRegistration threadPools = subsystem.registerSubModel(PathElement.pathElement(THREAD_POOL), BOUNDED_QUEUE_THREAD_POOL_DESC);
        threadPools.registerOperationHandler(ADD, BoundedQueueThreadPoolAdd.INSTANCE, BoundedQueueThreadPoolAdd.INSTANCE, false);
        threadPools.registerOperationHandler(REMOVE, BoundedQueueThreadPoolRemove.INSTANCE, BoundedQueueThreadPoolRemove.INSTANCE, false);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewConnectorSubsystemParser.INSTANCE);
    }

    private static ModelNode createEmptyAddOperation() {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, JCA);
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
            writeCachedConnectionManager(writer, node);
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

        private void writeCachedConnectionManager(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            if (has(node, CACHED_CONNECTION_MANAGER_DEBUG) || has(node, CACHED_CONNECTION_MANAGER_ERROR)) {
                writer.writeEmptyElement(Element.CACHED_CONNECTION_MANAGER.getLocalName());
                if (has(node, CACHED_CONNECTION_MANAGER_DEBUG))
                    writeAttribute(writer, Attribute.DEBUG, node.require(CACHED_CONNECTION_MANAGER_DEBUG));
                if (has(node, CACHED_CONNECTION_MANAGER_ERROR))
                    writeAttribute(writer, Attribute.ERROR, node.require(CACHED_CONNECTION_MANAGER_ERROR));
            }
        }

        private void writeDefaultWorkManager(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            if (node.hasDefined(THREAD_POOL)) {
                writer.writeStartElement(Element.DEFAULT_WORKMANAGER.getLocalName());
                for (Property prop : node.get(THREAD_POOL).asPropertyList()) {
                    if (LONG_RUNNING_THREADS.equals(prop.getName())) {
                        ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue(), Element.LONG_RUNNING_THREADS.getLocalName(), false);
                    }
                    if (SHORT_RUNNING_THREADS.equals(prop.getName())) {
                        ThreadsParser.getInstance().writeBoundedQueueThreadPool(writer, prop.getValue(), Element.SHORT_RUNNING_THREADS.getLocalName(), false);
                    }
                }
                writer.writeEndElement();
            }
            // if (hasAnyOf(node, DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL,
            // DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL)) {
            // writer.writeStartElement(Element.DEFAULT_WORKMANAGER.getLocalName());
            // if
            // (node.hasDefined(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL)) {
            // writer.writeStartElement(Element.LONG_RUNNING_THREADS.getLocalName());
            // (new NewThreadsParser()).writeThreadsElement(writer,
            // node.get(DEFAULT_WORKMANAGER_THREADS, LONG_RUNNING_THREADS).as);
            // writer.writeEndElement();
            // }
            // if
            // (node.hasDefined(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL))
            // {
            // writer.writeStartElement(Element.SHORT_RUNNING_THREADS.getLocalName());
            // (new NewThreadsParser()).writeThreadsElement(writer,
            // node.get(DEFAULT_WORKMANAGER_THREADS, SHORT_RUNNING_THREADS));
            // writer.writeEndElement();
            // }
            //
            // writer.writeEndElement();
            // }
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

            final ModelNode address = new ModelNode();
            address.add(org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM, JCA);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // Handle elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            final EnumSet<Element> requiredElement = EnumSet.of(Element.DEFAULT_WORKMANAGER);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case JCA_1_0: {
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
                                parseDefaultWorkManager(reader, address, list, subsystem);
                                requiredElement.remove(Element.DEFAULT_WORKMANAGER);
                                break;

                            }
                            case CACHED_CONNECTION_MANAGER: {
                                parseCcm(reader, subsystem);
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

        private void parseDefaultWorkManager(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
                final List<ModelNode> list, final ModelNode node) throws XMLStreamException {

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());

                switch (element) {
                    case LONG_RUNNING_THREADS: {
                        ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, parentAddress, list, THREAD_POOL, LONG_RUNNING_THREADS);
                        break;
                    }
                    case SHORT_RUNNING_THREADS: {
                        ThreadsParser.getInstance().parseBoundedQueueThreadPool(reader, parentAddress, list, THREAD_POOL, SHORT_RUNNING_THREADS);
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }

            }
            // Handle elements
            requireNoContent(reader);
        }

        private void parseBeanValidation(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
            requireSingleAttribute(reader, Attribute.ENABLED.getLocalName());
            final boolean value = reader.getAttributeValue(0) != null ? Boolean.parseBoolean(reader.getAttributeValue(0))
                    : true;
            requireNoContent(reader);

            node.get(BEAN_VALIDATION_ENABLED).set(value);
            // Don't add a requireNoContent here as readBooleanAttributeElement
            // already performs that check.
        }

        private void parseCcm(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {

            final boolean debug = Boolean.parseBoolean(reader.getAttributeValue("", Attribute.DEBUG.getLocalName()));
            final boolean error = Boolean.parseBoolean(reader.getAttributeValue("", Attribute.ERROR.getLocalName()));

            node.get(CACHED_CONNECTION_MANAGER_DEBUG).set(debug);
            node.get(CACHED_CONNECTION_MANAGER_ERROR).set(error);

            requireNoContent(reader);
        }
    }

    private static class ConnectorSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final ConnectorSubsystemDescribeHandler INSTANCE = new ConnectorSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode add = createEmptyAddOperation();
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

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
            if (model.hasDefined(CACHED_CONNECTION_MANAGER_DEBUG)) {
                add.get(CACHED_CONNECTION_MANAGER_DEBUG).set(model.get(CACHED_CONNECTION_MANAGER_DEBUG));
            }
            if (model.hasDefined(CACHED_CONNECTION_MANAGER_ERROR)) {
                add.get(CACHED_CONNECTION_MANAGER_ERROR).set(model.get(CACHED_CONNECTION_MANAGER_ERROR));
            }

            final ModelNode result = context.getResult();
            result.add(add);

            if (model.hasDefined(THREAD_POOL)) {
                ModelNode pools = model.get(THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    if (poolProp.getName().equals(LONG_RUNNING_THREADS)) {
                        addBoundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_POOL, LONG_RUNNING_THREADS));
                    } else if (poolProp.getName().equals(SHORT_RUNNING_THREADS)) {
                        addBoundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_POOL, SHORT_RUNNING_THREADS));
                    }
                }
            }

            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
