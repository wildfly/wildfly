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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import static org.jboss.as.txn.CommonAttributes.BINDING;
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
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The transaction managment extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
public class TransactionExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.txn");

    public static final String SUBSYSTEM_NAME = "transactions";
    private static final TransactionSubsystemParser parser = new TransactionSubsystemParser();

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        log.debug("Initializing Transactions Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(TransactionSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, TransactionSubsystemAdd.INSTANCE, TransactionSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, TransactionDescribeHandler.INSTANCE, TransactionDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
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

            final ModelNode subsystem = createEmptyAddOperation();
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
                                final ModelNode model = parseRecoveryEnvironmentElement(reader);
                                subsystem.get(CommonAttributes.RECOVERY_ENVIRONMENT).set(model) ;
                                break;
                            }
                            case CORE_ENVIRONMENT: {
                                final ModelNode model = parseCoreEnvironmentElement(reader);
                                subsystem.get(CommonAttributes.CORE_ENVIRONMENT).set(model) ;
                                break;
                            }
                            case COORDINATOR_ENVIRONMENT: {
                                final ModelNode model = parseCoordinatorEnvironmentElement(reader);
                                subsystem.get(CommonAttributes.COORDINATOR_ENVIRONMENT).set(model) ;
                                break;
                            }
                            case OBJECT_STORE: {
                                final ModelNode model = parseObjectStoreEnvironmentElement(reader);
                                subsystem.get(CommonAttributes.OBJECT_STORE).set(model) ;
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

        static ModelNode parseObjectStoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode store = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case RELATIVE_TO:
                        store.get(RELATIVE_TO).set(value);
                        break;
                    case PATH:
                        store.get(PATH).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            // Handle elements
            requireNoContent(reader);
            return store;
        }

        static ModelNode parseCoordinatorEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode coordinator = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLE_STATISTICS:
                        coordinator.get(ENABLE_STATISTICS).set(value);
                        break;
                    case ENABLE_TSM_STATUS:
                        coordinator.get(ENABLE_TSM_STATUS).set(value);
                        break;
                    case DEFAULT_TIMEOUT:
                        coordinator.get(DEFAULT_TIMEOUT).set(value);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            // Handle elements
            requireNoContent(reader);
            return coordinator;
        }

        /**
         * Handle the core-environment element and children
         * @param reader
         * @return ModelNode for the core-environment
         * @throws XMLStreamException
         */
        static ModelNode parseCoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            final ModelNode env = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NODE_IDENTIFIER:
                        env.get(NODE_IDENTIFIER).set(value);
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
                    env.get(CommonAttributes.PROCESS_ID).set(processId);

                    break;
                  }
                  default:
                     throw unexpectedElement(reader);
                }
            }
            if (! required.isEmpty()) {
                throw missingRequired(reader, required);
            }
            return env;
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

        static ModelNode parseRecoveryEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode env = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BINDING:
                        env.get(BINDING).set(value);
                        break;
                    case STATUS_BINDING:
                        env.get(STATUS_BINDING).set(value);
                        break;
                    case RECOVERY_LISTENER:
                        env.get(RECOVERY_LISTENER).set(value);
                        break;
                    default:
                        unexpectedAttribute(reader, i);
                }
            }
            if(! env.has(BINDING)) {
                throw missingRequired(reader, Collections.singleton(Attribute.BINDING));
            }
            // Handle elements
            requireNoContent(reader);
            return env;
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

            ModelNode node = context.getModelNode();

            if (has(node, CORE_ENVIRONMENT)) {
                writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());
                final ModelNode core = node.get(CORE_ENVIRONMENT);
                if (has(core, PROCESS_ID)) {
                    writeProcessId(writer, core.get(PROCESS_ID));
                }
                if (has(core, NODE_IDENTIFIER)) {
                    writeAttribute(writer, Attribute.NODE_IDENTIFIER, core.get(NODE_IDENTIFIER));
                }
                writer.writeEndElement();
            }
            if (has(node, RECOVERY_ENVIRONMENT)) {
                writer.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
                final ModelNode env = node.get(RECOVERY_ENVIRONMENT);
                if (has(env, BINDING)) {
                    writeAttribute(writer, Attribute.BINDING, env.get(BINDING));
                }
                if (has(env, STATUS_BINDING)) {
                    writeAttribute(writer, Attribute.STATUS_BINDING, env.get(STATUS_BINDING));
                }
                if (has(env, RECOVERY_LISTENER)) {
                    writeAttribute(writer, Attribute.RECOVERY_LISTENER, env.get(RECOVERY_LISTENER));
                }
                writer.writeEndElement();
            }
            if (has(node, COORDINATOR_ENVIRONMENT)) {
                writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());
                final ModelNode env = node.get(COORDINATOR_ENVIRONMENT);
                if (has(env, ENABLE_STATISTICS)) {
                    writeAttribute(writer, Attribute.ENABLE_STATISTICS, env.get(ENABLE_STATISTICS));
                }
                if (has(env, ENABLE_TSM_STATUS)) {
                    writeAttribute(writer, Attribute.ENABLE_TSM_STATUS, env.get(ENABLE_TSM_STATUS));
                }
                if (has(env, DEFAULT_TIMEOUT)) {
                    writeAttribute(writer, Attribute.DEFAULT_TIMEOUT, env.get(DEFAULT_TIMEOUT));
                }
                writer.writeEndElement();
            }
            if (has(node, OBJECT_STORE)) {
                writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
                final ModelNode env = node.get(OBJECT_STORE);
                if (has(env, RELATIVE_TO)) {
                    writeAttribute(writer, Attribute.RELATIVE_TO, env.get(RELATIVE_TO));
                }
                if (has(env, PATH)) {
                    writeAttribute(writer, Attribute.PATH, env.get(PATH));
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }
        private void writeProcessId(final XMLExtendedStreamWriter writer, final ModelNode value) throws XMLStreamException {
            writer.writeStartElement(Element.PROCESS_ID.getLocalName());
            if(has(value, Element.UUID.getLocalName())) {
                writer.writeEmptyElement(Element.UUID.getLocalName());
            }
            else if(has(value, Element.SOCKET.getLocalName())) {
                writer.writeStartElement(Element.SOCKET.getLocalName());
                if (has(value, BINDING)) {
                    writeAttribute(writer, Attribute.BINDING, value.get(BINDING));
                }
                if (has(value, SOCKET_PROCESS_ID_MAX_PORTS)) {
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
            ModelNode add = createEmptyAddOperation();

            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            if (model.hasDefined(CORE_ENVIRONMENT)) {
                add.get(CORE_ENVIRONMENT).set(model.get(CORE_ENVIRONMENT));
            }
            if (model.hasDefined(RECOVERY_ENVIRONMENT)) {
                add.get(RECOVERY_ENVIRONMENT).set(model.get(RECOVERY_ENVIRONMENT));
            }
            if (model.hasDefined(COORDINATOR_ENVIRONMENT)) {
                add.get(COORDINATOR_ENVIRONMENT).set(model.get(COORDINATOR_ENVIRONMENT));
            }

            if (model.hasDefined(OBJECT_STORE)) {
                add.get(OBJECT_STORE).set(model.get(OBJECT_STORE));
            }

            context.getResult().add(add);

            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }


}
