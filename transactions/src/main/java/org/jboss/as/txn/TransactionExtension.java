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
                                parseRecoveryEnvironmentElement(reader, subsystem);
                                break;
                            }
                            case CORE_ENVIRONMENT: {
                                parseCoreEnvironmentElement(reader, subsystem);
                                break;
                            }
                            case COORDINATOR_ENVIRONMENT: {
                                parseCoordinatorEnvironmentElement(reader, subsystem);
                                break;
                            }
                            case OBJECT_STORE: {
                                parseObjectStoreEnvironmentElementAndEnrichOperation(reader, subsystem);
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
                        TransactionSubsystemAdd.OBJECT_STORE_RELATIVE_TO.parseAndSetParameter(value, operation, location);
                        break;
                    case PATH:
                        TransactionSubsystemAdd.OBJECT_STORE_PATH.parseAndSetParameter(value, operation, location);
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
                        TransactionSubsystemAdd.ENABLE_STATISTICS.parseAndSetParameter(value, operation, location);
                        break;
                    case ENABLE_TSM_STATUS:
                        TransactionSubsystemAdd.ENABLE_TSM_STATUS.parseAndSetParameter(value, operation, location);
                        break;
                    case DEFAULT_TIMEOUT:
                        TransactionSubsystemAdd.DEFAULT_TIMEOUT.parseAndSetParameter(value, operation, location);
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
        static void parseCoreEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NODE_IDENTIFIER:
                        TransactionSubsystemAdd.NODE_IDENTIFIER.parseAndSetParameter(value, operation, reader.getLocation());
                        break;
                    case PATH:
                        TransactionSubsystemAdd.PATH.parseAndSetParameter(value, operation, reader.getLocation());
                        break;
                    case RELATIVE_TO:
                        TransactionSubsystemAdd.RELATIVE_TO.parseAndSetParameter(value, operation, reader.getLocation());
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
                      coreEnvironmentAdd.get(TransactionSubsystemAdd.PROCESS_ID_UUID.getName()).set(true);
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
                        TransactionSubsystemAdd.PROCESS_ID_SOCKET_BINDING.parseAndSetParameter(value, coreEnvironmentAdd, reader.getLocation());
                        break;
                    case SOCKET_PROCESS_ID_MAX_PORTS:
                        TransactionSubsystemAdd.PROCESS_ID_SOCKET_MAX_PORTS.parseAndSetParameter(value, coreEnvironmentAdd, reader.getLocation());
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

        static void parseRecoveryEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

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
                        TransactionSubsystemAdd.BINDING.parseAndSetParameter(value, operation, location);
                        break;
                    case STATUS_BINDING:
                        TransactionSubsystemAdd.STATUS_BINDING.parseAndSetParameter(value, operation, location);
                        break;
                    case RECOVERY_LISTENER:
                        TransactionSubsystemAdd.RECOVERY_LISTENER.parseAndSetParameter(value, operation, location);
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

        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

            ModelNode node = context.getModelNode();


            writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());

            TransactionSubsystemAdd.NODE_IDENTIFIER.marshallAsAttribute(node, writer);
            TransactionSubsystemAdd.PATH.marshallAsAttribute(node, writer);
            TransactionSubsystemAdd.RELATIVE_TO.marshallAsAttribute(node, writer);

            writeProcessId(writer, node);

            writer.writeEndElement();

            if (TransactionSubsystemAdd.BINDING.isMarshallable(node) ||
                    TransactionSubsystemAdd.STATUS_BINDING.isMarshallable(node) ||
                    TransactionSubsystemAdd.RECOVERY_LISTENER.isMarshallable(node)) {
                writer.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
                TransactionSubsystemAdd.BINDING.marshallAsAttribute(node, writer);

                TransactionSubsystemAdd.STATUS_BINDING.marshallAsAttribute(node, writer);

                TransactionSubsystemAdd.RECOVERY_LISTENER.marshallAsAttribute(node, writer);

                writer.writeEndElement();
            }
            if (TransactionSubsystemAdd.ENABLE_STATISTICS.isMarshallable(node)
                    || TransactionSubsystemAdd.ENABLE_TSM_STATUS.isMarshallable(node)
                    || TransactionSubsystemAdd.DEFAULT_TIMEOUT.isMarshallable(node)) {

                writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());

                TransactionSubsystemAdd.ENABLE_STATISTICS.marshallAsAttribute(node, writer);
                TransactionSubsystemAdd.ENABLE_TSM_STATUS.marshallAsAttribute(node, writer);
                TransactionSubsystemAdd.DEFAULT_TIMEOUT.marshallAsAttribute(node, writer);

                writer.writeEndElement();
            }

            if (TransactionSubsystemAdd.OBJECT_STORE_RELATIVE_TO.isMarshallable(node)
                    || TransactionSubsystemAdd.OBJECT_STORE_PATH.isMarshallable(node)) {
                writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
                TransactionSubsystemAdd.OBJECT_STORE_PATH.marshallAsAttribute(node, writer);
                TransactionSubsystemAdd.OBJECT_STORE_RELATIVE_TO.marshallAsAttribute(node, writer);
                writer.writeEndElement();
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
            if(value.get(TransactionSubsystemAdd.PROCESS_ID_UUID.getName()).asBoolean()) {
                writer.writeEmptyElement(Element.UUID.getLocalName());
            } else {
                writer.writeStartElement(Element.SOCKET.getLocalName());
                TransactionSubsystemAdd.PROCESS_ID_SOCKET_BINDING.marshallAsAttribute(value, writer);
                TransactionSubsystemAdd.PROCESS_ID_SOCKET_MAX_PORTS.marshallAsAttribute(value, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

}
