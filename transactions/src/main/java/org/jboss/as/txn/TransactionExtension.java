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

package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.txn.CommonAttributes.BINDING;
import static org.jboss.as.txn.CommonAttributes.CONFIGURATION;
import static org.jboss.as.txn.CommonAttributes.COORDINATOR_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.CORE_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.DEFAULT_TIMEOUT;
import static org.jboss.as.txn.CommonAttributes.ENABLE_STATISTICS;
import static org.jboss.as.txn.CommonAttributes.ENABLE_TSM_STATUS;
import static org.jboss.as.txn.CommonAttributes.NODE_IDENTIFIER;
import static org.jboss.as.txn.CommonAttributes.OBJECT_STORE;
import static org.jboss.as.txn.CommonAttributes.PROCESS_ID;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_LISTENER;
import static org.jboss.as.txn.CommonAttributes.SOCKET_PROCESS_ID_MAX_PORTS;
import static org.jboss.as.txn.CommonAttributes.STATUS_BINDING;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.Collections;
import java.util.EnumSet;
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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The transaction management extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
public class TransactionExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "transactions";
    private static final TransactionSubsystemParser parser = new TransactionSubsystemParser();

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debug("Initializing Transactions Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(TransactionSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, TransactionSubsystemAdd.INSTANCE, TransactionSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, TransactionDescribeHandler.INSTANCE, TransactionDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration recoveryEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, RECOVERY_ENVIRONMENT),
                TransactionSubsystemProviders.RECOVERY_ENVIRONMENT_DESC);
        recoveryEnv.registerOperationHandler(ADD, RecoveryEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_RECOVERY_ENVIRONMENT_DESC, false);
        recoveryEnv.registerOperationHandler(REMOVE, RecoveryEnvironmentRemove.INSTANCE, TransactionSubsystemProviders.REMOVE_RECOVERY_ENVIRONMENT_DESC, false);

        final ManagementResourceRegistration coreEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, CORE_ENVIRONMENT),
                TransactionSubsystemProviders.CORE_ENVIRONMENT_DESC);
        coreEnv.registerOperationHandler(ADD, CoreEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_CORE_ENVIRONMENT_DESC, false);
        coreEnv.registerOperationHandler(REMOVE, CoreEnvironmentRemove.INSTANCE, TransactionSubsystemProviders.REMOVE_CORE_ENVIRONMENT_DESC, false);

        final ManagementResourceRegistration coordinatorEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, COORDINATOR_ENVIRONMENT),
                TransactionSubsystemProviders.COORDINATOR_ENVIRONMENT_DESC);
        coordinatorEnv.registerOperationHandler(ADD, CoordinatorEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_COORDINATOR_ENVIRONMENT_DESC, false);
        coordinatorEnv.registerOperationHandler(REMOVE, CoordinatorEnvironmentRemove.INSTANCE, TransactionSubsystemProviders.REMOVE_COORDINATOR_ENVIRONMENT_DESC, false);

        final ManagementResourceRegistration objectStore = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, OBJECT_STORE),
                TransactionSubsystemProviders.OBJECT_STORE_DESC);
        objectStore.registerOperationHandler(ADD, ObjectStoreAdd.INSTANCE, TransactionSubsystemProviders.ADD_OBJECT_STORE_DESC, false);
        objectStore.registerOperationHandler(REMOVE, ObjectStoreRemove.INSTANCE, TransactionSubsystemProviders.REMOVE_OBJECT_STORE_DESC, false);

        for (TxStatsHandler.TxStat stat : EnumSet.allOf(TxStatsHandler.TxStat.class)) {
            registration.registerMetric(stat.toString(), TxStatsHandler.INSTANCE);
        }
        subsystem.registerXMLElementWriter(parser);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    private static ModelNode createEmptyAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    static class TransactionSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw unexpectedAttribute(reader, 0);
            }

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);

            list.add(subsystem);

            //Object store is always required
            final ModelNode objectStoreNode = address.clone();
            final ModelNode objectStoreOperation = new ModelNode();
            objectStoreOperation.get(OP).set(ADD);
            objectStoreNode.add(CONFIGURATION, OBJECT_STORE);
            objectStoreNode.protect();
            objectStoreOperation.get(OP_ADDR).set(objectStoreNode);

            final ModelNode coordinatorNode = address.clone();
            final ModelNode coordinatorOperatrion = new ModelNode();
            coordinatorOperatrion.get(OP).set(ADD);
            coordinatorNode.add(CONFIGURATION, COORDINATOR_ENVIRONMENT);
            coordinatorNode.protect();
            coordinatorOperatrion.get(OP_ADDR).set(coordinatorNode);


            // elements
            final EnumSet<Element> required = EnumSet.of(Element.RECOVERY_ENVIRONMENT, Element.CORE_ENVIRONMENT);
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case TRANSACTIONS_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        required.remove(element);
                        if (! encountered.add(element)) {
                            throw unexpectedElement(reader);
                        }
                        switch (element) {
                            case RECOVERY_ENVIRONMENT: {
                                parseRecoveryEnvironmentElement(reader, list, address);
                                break;
                            }
                            case CORE_ENVIRONMENT: {
                                parseCoreEnvironmentElement(reader, list, address);
                                break;
                            }
                            case COORDINATOR_ENVIRONMENT: {
                                parseCoordinatorEnvironmentElement(reader, coordinatorOperatrion);
                                break;
                            }
                            case OBJECT_STORE: {
                                parseObjectStoreEnvironmentElementAndEnrichOperation(reader, objectStoreOperation);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            list.add(objectStoreOperation);
            list.add(coordinatorOperatrion);
            if (! required.isEmpty()) {
                throw missingRequiredElement(reader, required);
            }
        }

        static void parseObjectStoreEnvironmentElementAndEnrichOperation(final XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case RELATIVE_TO:
                        operation.get(RELATIVE_TO).set(value);
                        break;
                    case PATH:
                        operation.get(PATH).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            // Handle elements
            requireNoContent(reader);

        }

        static void parseCoordinatorEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLE_STATISTICS:
                        operation.get(ENABLE_STATISTICS).set(value);
                        break;
                    case ENABLE_TSM_STATUS:
                        operation.get(ENABLE_TSM_STATUS).set(value);
                        break;
                    case DEFAULT_TIMEOUT:
                        operation.get(DEFAULT_TIMEOUT).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            // Handle elements
            requireNoContent(reader);

        }

        /**
         * Handle the core-environment element and children
         * @param reader
         * @return ModelNode for the core-environment
         * @throws XMLStreamException
         */
        static void parseCoreEnvironmentElement(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException {
            final ModelNode env = parentAddress.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            env.add(CONFIGURATION, CORE_ENVIRONMENT);
            env.protect();

            operation.get(OP_ADDR).set(env);
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NODE_IDENTIFIER:
                        operation.get(NODE_IDENTIFIER).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            // elements
            final EnumSet<Element> required = EnumSet.of(Element.PROCESS_ID);
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                required.remove(element);
                switch (element) {
                  case PROCESS_ID : {
                      if (!encountered.add(element)) {
                          throw duplicateNamedElement(reader, reader.getLocalName());
                      }
                    ModelNode processId = parseProcessIdEnvironmentElement(reader);
                    operation.get(CommonAttributes.PROCESS_ID).set(processId);

                    break;
                  }
                  default:
                     throw unexpectedElement(reader);
                }
            }
            if (! required.isEmpty()) {
                throw missingRequired(reader, required);
            }
            list.add(operation);
        }

        /**
         * Handle the process-id child elements
         * @param reader
         * @return
         * @throws XMLStreamException
         */
        static ModelNode parseProcessIdEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            final ModelNode processId = new ModelNode();

            // elements
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                  case UUID:
                      if (!encountered.add(element)) {
                          throw duplicateNamedElement(reader, reader.getLocalName());
                      }
                      processId.get(CommonAttributes.UUID).set(element.getLocalName());
                      requireNoContent(reader);
                      break;
                  case SOCKET: {
                      if (!encountered.add(element)) {
                          throw duplicateNamedElement(reader, reader.getLocalName());
                      }
                    ModelNode socketId = parseSocketProcessIdElement(reader);
                    processId.get(CommonAttributes.SOCKET).set(socketId);
                    break;
                  }
                  default:
                     throw unexpectedElement(reader);
               }
            }

            return processId;
        }

        static ModelNode parseSocketProcessIdElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            final ModelNode socketId = new ModelNode();
            final int count = reader.getAttributeCount();
            final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case BINDING:
                        socketId.get(BINDING).set(value);
                        break;
                    case SOCKET_PROCESS_ID_MAX_PORTS:
                        socketId.get(SOCKET_PROCESS_ID_MAX_PORTS).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            if (! required.isEmpty()) {
                throw missingRequired(reader, required);
            }
            // Handle elements
            requireNoContent(reader);
            return socketId;
        }

        static void parseRecoveryEnvironmentElement(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException {
            final ModelNode recoveryEnvAddress = parentAddress.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            recoveryEnvAddress.add(CONFIGURATION, RECOVERY_ENVIRONMENT);
            recoveryEnvAddress.protect();

            operation.get(OP_ADDR).set(recoveryEnvAddress);

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BINDING:
                        operation.get(BINDING).set(value);
                        break;
                    case STATUS_BINDING:
                        operation.get(STATUS_BINDING).set(value);
                        break;
                    case RECOVERY_LISTENER:
                        operation.get(RECOVERY_LISTENER).set(value);
                        break;
                    default:
                        unexpectedAttribute(reader, i);
                }
            }
            if(! operation.hasDefined(BINDING)) {
                throw missingRequired(reader, Collections.singleton(Attribute.BINDING));
            }
            // Handle elements
            requireNoContent(reader);
            list.add(operation);
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

            ModelNode node = context.getModelNode();

            if (hasDefined(node, CONFIGURATION)) {
                List<Property> configurations = node.get(CONFIGURATION).asPropertyList();
                for (Property config : configurations) {
                    if (config.getName().equals(CORE_ENVIRONMENT) &&
                            config.getValue().isDefined()) {
                        writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());
                        final ModelNode core = config.getValue();
                        if (hasDefined(core, NODE_IDENTIFIER)) {
                            writeAttribute(writer, Attribute.NODE_IDENTIFIER, core.get(NODE_IDENTIFIER));
                        }
                        if (hasDefined(core, PROCESS_ID)) {
                            writeProcessId(writer, core.get(PROCESS_ID));
                        }
                        writer.writeEndElement();
                    }
                    if (config.getName().equals(RECOVERY_ENVIRONMENT) && config.getValue().isDefined()) {
                        writer.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
                        final ModelNode env = config.getValue();
                        if (hasDefined(env, BINDING)) {
                            writeAttribute(writer, Attribute.BINDING, env.get(BINDING));
                        }
                        if (hasDefined(env, STATUS_BINDING)) {
                            writeAttribute(writer, Attribute.STATUS_BINDING, env.get(STATUS_BINDING));
                        }
                        if (hasDefined(env, RECOVERY_LISTENER)) {
                            writeAttribute(writer, Attribute.RECOVERY_LISTENER, env.get(RECOVERY_LISTENER));
                        }
                        writer.writeEndElement();
                    }
                    if (config.getName().equals(COORDINATOR_ENVIRONMENT) && config.getValue().isDefined()) {
                        writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());
                        final ModelNode env = config.getValue();
                        if (hasDefined(env, ENABLE_STATISTICS)) {
                            writeAttribute(writer, Attribute.ENABLE_STATISTICS, env.get(ENABLE_STATISTICS));
                        }
                        if (hasDefined(env, ENABLE_TSM_STATUS)) {
                            writeAttribute(writer, Attribute.ENABLE_TSM_STATUS, env.get(ENABLE_TSM_STATUS));
                        }
                        if (hasDefined(env, DEFAULT_TIMEOUT)) {
                            writeAttribute(writer, Attribute.DEFAULT_TIMEOUT, env.get(DEFAULT_TIMEOUT));
                        }
                        writer.writeEndElement();
                    }
                    if (config.getName().equals(OBJECT_STORE) && config.getValue().isDefined()) {
                        writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
                        final ModelNode env = config.getValue();
                        if (hasDefined(env, RELATIVE_TO)) {
                            writeAttribute(writer, Attribute.RELATIVE_TO, env.get(RELATIVE_TO));
                        }
                        if (hasDefined(env, PATH)) {
                            writeAttribute(writer, Attribute.PATH, env.get(PATH));
                        }
                        writer.writeEndElement();
                    }
                }
            }

            writer.writeEndElement();
        }

        private boolean hasDefined(ModelNode node, String name) {
            return node.hasDefined(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }
        private void writeProcessId(final XMLExtendedStreamWriter writer, final ModelNode value) throws XMLStreamException {
            writer.writeStartElement(Element.PROCESS_ID.getLocalName());
            if(hasDefined(value, Element.UUID.getLocalName())) {
                writer.writeEmptyElement(Element.UUID.getLocalName());
            }
            else if(hasDefined(value, Element.SOCKET.getLocalName())) {
                writer.writeStartElement(Element.SOCKET.getLocalName());
                if (hasDefined(value, BINDING)) {
                    writeAttribute(writer, Attribute.BINDING, value.get(BINDING));
                }
                if (hasDefined(value, SOCKET_PROCESS_ID_MAX_PORTS)) {
                    writeAttribute(writer, Attribute.SOCKET_PROCESS_ID_MAX_PORTS, value.get(SOCKET_PROCESS_ID_MAX_PORTS));
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private static class TransactionDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final TransactionDescribeHandler INSTANCE = new TransactionDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            /*ModelNode add = createEmptyAddOperation();

            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            context.getResult().add(add);

            context.completeStep();         */


        final ModelNode result = context.getResult();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        if (subModel.hasDefined(CommonAttributes.CONFIGURATION)) {
            subsystemAdd.get(CommonAttributes.CONFIGURATION).set(subModel.get(CommonAttributes.CONFIGURATION));
        }
        result.add(subsystemAdd);
        /*if (subModel.hasDefined(Constants.CONNECTOR)) {
            for (final Property connector : subModel.get(Constants.CONNECTOR).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(Constants.CONNECTOR, connector.getName());
                result.add(WebConnectorAdd.getRecreateOperation(address, connector.getValue()));
            }
        }
        if (subModel.hasDefined(Constants.VIRTUAL_SERVER)) {
            for (final Property host : subModel.get(Constants.VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(Constants.VIRTUAL_SERVER, host.getName());
                result.add(WebVirtualHostAdd.getAddOperation(address, host.getValue()));
            }
        } */
        context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }


}
