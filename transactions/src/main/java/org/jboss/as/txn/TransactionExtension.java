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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
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
import static org.jboss.as.txn.CommonAttributes.OBJECT_STORE;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_LISTENER;
import static org.jboss.as.txn.CommonAttributes.STATUS_BINDING;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
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

        final EnumSet<OperationEntry.Flag> reloadFlags = EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES);
        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(TransactionSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, TransactionSubsystemAdd.INSTANCE, TransactionSubsystemProviders.SUBSYSTEM_ADD, reloadFlags);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, TransactionSubsystemProviders.SUBSYSTEM_REMOVE, reloadFlags);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        for (TxStatsHandler.TxStat stat : EnumSet.allOf(TxStatsHandler.TxStat.class)) {
            registration.registerMetric(stat.toString(), TxStatsHandler.INSTANCE);
        }

        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        final ManagementResourceRegistration recoveryEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, RECOVERY_ENVIRONMENT),
                TransactionSubsystemProviders.RECOVERY_ENVIRONMENT_DESC);
        recoveryEnv.registerOperationHandler(ADD, RecoveryEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_RECOVERY_ENVIRONMENT_DESC, reloadFlags);
        recoveryEnv.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, TransactionSubsystemProviders.REMOVE_RECOVERY_ENVIRONMENT_DESC, reloadFlags);

        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        final ManagementResourceRegistration coreEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, CORE_ENVIRONMENT),
                TransactionSubsystemProviders.CORE_ENVIRONMENT_DESC);
        coreEnv.registerOperationHandler(ADD, CoreEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_CORE_ENVIRONMENT_DESC, reloadFlags);
        coreEnv.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, TransactionSubsystemProviders.REMOVE_CORE_ENVIRONMENT_DESC, reloadFlags);

        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        final ManagementResourceRegistration coordinatorEnv = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, COORDINATOR_ENVIRONMENT),
                TransactionSubsystemProviders.COORDINATOR_ENVIRONMENT_DESC);
        coordinatorEnv.registerOperationHandler(ADD, CoordinatorEnvironmentAdd.INSTANCE, TransactionSubsystemProviders.ADD_COORDINATOR_ENVIRONMENT_DESC, reloadFlags);
        coordinatorEnv.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, TransactionSubsystemProviders.REMOVE_COORDINATOR_ENVIRONMENT_DESC, reloadFlags);

        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        final ManagementResourceRegistration objectStore = registration.registerSubModel(PathElement.pathElement(CONFIGURATION, OBJECT_STORE),
                TransactionSubsystemProviders.OBJECT_STORE_DESC);
        objectStore.registerOperationHandler(ADD, ObjectStoreAdd.INSTANCE, TransactionSubsystemProviders.ADD_OBJECT_STORE_DESC, reloadFlags);
        objectStore.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, TransactionSubsystemProviders.REMOVE_OBJECT_STORE_DESC, reloadFlags);

        subsystem.registerXMLElementWriter(parser);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
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
                final Location location = reader.getLocation();
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case RELATIVE_TO:
                        ObjectStoreAdd.RELATIVE_TO.parseAndSetParameter(value, operation, location);
                        break;
                    case PATH:
                        ObjectStoreAdd.PATH.parseAndSetParameter(value, operation, location);
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
                final Location location = reader.getLocation();
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLE_STATISTICS:
                        CoordinatorEnvironmentAdd.ENABLE_STATISTICS.parseAndSetParameter(value, operation, location);
                        break;
                    case ENABLE_TSM_STATUS:
                        CoordinatorEnvironmentAdd.ENABLE_TSM_STATUS.parseAndSetParameter(value, operation, location);
                        break;
                    case DEFAULT_TIMEOUT:
                        CoordinatorEnvironmentAdd.DEFAULT_TIMEOUT.parseAndSetParameter(value, operation, location);
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
            env.add(CONFIGURATION, CORE_ENVIRONMENT);
            env.protect();

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(env);

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NODE_IDENTIFIER:
                        CoreEnvironmentAdd.NODE_IDENTIFIER.parseAndSetParameter(value, operation, reader.getLocation());
                        break;
                    case PATH:
                        CoreEnvironmentAdd.PATH.parseAndSetParameter(value, operation, reader.getLocation());
                        break;
                    case RELATIVE_TO:
                        CoreEnvironmentAdd.RELATIVE_TO.parseAndSetParameter(value, operation, reader.getLocation());
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
                      parseProcessIdEnvironmentElement(reader, operation);
                      break;
                  }
                  default:
                     throw unexpectedElement(reader);
                }
            }
            if (! required.isEmpty()) {
                throw missingRequiredElement(reader, required);
            }
            list.add(operation);
        }

        /**
         * Handle the process-id child elements
         *
         * @param reader
         * @param coreEnvironmentAdd
         * @return
         * @throws XMLStreamException
         */
        static void parseProcessIdEnvironmentElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {

            // elements
            boolean encountered = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                  case UUID:
                      if (encountered) {
                          throw unexpectedElement(reader);
                      }
                      encountered = true;
                      coreEnvironmentAdd.get(CoreEnvironmentAdd.PROCESS_ID_UUID.getName()).set(true);
                      requireNoContent(reader);
                      break;
                  case SOCKET: {
                      if (encountered) {
                          throw unexpectedElement(reader);
                      }
                      encountered = true;
                      parseSocketProcessIdElement(reader, coreEnvironmentAdd);
                      break;
                  }
                  default:
                     throw unexpectedElement(reader);
               }
            }

            if (!encountered) {
                throw missingOneOf(reader, EnumSet.of(Element.UUID, Element.SOCKET));
            }
        }

        static void parseSocketProcessIdElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {

            final int count = reader.getAttributeCount();
            final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case BINDING:
                        CoreEnvironmentAdd.PROCESS_ID_SOCKET_BINDING.parseAndSetParameter(value, coreEnvironmentAdd, reader.getLocation());
                        break;
                    case SOCKET_PROCESS_ID_MAX_PORTS:
                        CoreEnvironmentAdd.PROCESS_ID_SOCKET_MAX_PORTS.parseAndSetParameter(value, coreEnvironmentAdd, reader.getLocation());
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
        }

        static void parseRecoveryEnvironmentElement(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException {
            final ModelNode recoveryEnvAddress = parentAddress.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            recoveryEnvAddress.add(CONFIGURATION, RECOVERY_ENVIRONMENT);
            recoveryEnvAddress.protect();

            operation.get(OP_ADDR).set(recoveryEnvAddress);

            Set<Attribute> required = EnumSet.of(Attribute.BINDING, Attribute.STATUS_BINDING);
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Location location = reader.getLocation();
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case BINDING:
                        RecoveryEnvironmentAdd.BINDING.parseAndSetParameter(value, operation, location);
                        break;
                    case STATUS_BINDING:
                        RecoveryEnvironmentAdd.STATUS_BINDING.parseAndSetParameter(value, operation, location);
                        break;
                    case RECOVERY_LISTENER:
                        RecoveryEnvironmentAdd.RECOVERY_LISTENER.parseAndSetParameter(value, operation, location);
                        break;
                    default:
                        unexpectedAttribute(reader, i);
                }
            }

            if(! required.isEmpty()) {
                throw missingRequired(reader, required);
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
                    if (config.getName().equals(CORE_ENVIRONMENT)) {
                        writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());

                        final ModelNode core = config.getValue();
                        CoreEnvironmentAdd.NODE_IDENTIFIER.marshallAsAttribute(core, writer);
                        CoreEnvironmentAdd.PATH.marshallAsAttribute(core, writer);
                        CoreEnvironmentAdd.RELATIVE_TO.marshallAsAttribute(core, writer);

                        writeProcessId(writer, core);

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
                    if (config.getName().equals(COORDINATOR_ENVIRONMENT)) {
                        final ModelNode env = config.getValue();
                        if (CoordinatorEnvironmentAdd.ENABLE_STATISTICS.isMarshallable(env)
                                || CoordinatorEnvironmentAdd.ENABLE_TSM_STATUS.isMarshallable(env)
                                || CoordinatorEnvironmentAdd.DEFAULT_TIMEOUT.isMarshallable(env)) {

                            writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());

                            CoordinatorEnvironmentAdd.ENABLE_STATISTICS.marshallAsAttribute(env, writer);
                            CoordinatorEnvironmentAdd.ENABLE_TSM_STATUS.marshallAsAttribute(env, writer);
                            CoordinatorEnvironmentAdd.DEFAULT_TIMEOUT.marshallAsAttribute(env, writer);

                            writer.writeEndElement();
                        }
                    }
                    if (config.getName().equals(OBJECT_STORE)) {
                        final ModelNode env = config.getValue();
                        if (ObjectStoreAdd.RELATIVE_TO.isMarshallable(env)
                                || ObjectStoreAdd.PATH.isMarshallable(env)) {
                            writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
                            ObjectStoreAdd.PATH.marshallAsAttribute(env, writer);
                            ObjectStoreAdd.RELATIVE_TO.marshallAsAttribute(env, writer);
                            writer.writeEndElement();
                        }
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
            if(value.get(CoreEnvironmentAdd.PROCESS_ID_UUID.getName()).asBoolean()) {
                writer.writeEmptyElement(Element.UUID.getLocalName());
            } else {
                writer.writeStartElement(Element.SOCKET.getLocalName());
                CoreEnvironmentAdd.PROCESS_ID_SOCKET_BINDING.marshallAsAttribute(value, writer);
                CoreEnvironmentAdd.PROCESS_ID_SOCKET_MAX_PORTS.marshallAsAttribute(value, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

}
