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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.txn.CommonAttributes.*;


import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class NewTransactionExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "transactions";
    private static final TransactionSubsystemParser parser = new TransactionSubsystemParser();

    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(TransactionSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewTransactionSubsystemAdd.INSTANCE, TransactionSubsystemProviders.SUBSYSTEM_ADD, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser, parser);
    }

    static class TransactionSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(SUBSYSTEM, SUBSYSTEM_NAME);

            // elements
            final EnumSet<Element> required = EnumSet.of(Element.RECOVERY_ENVIRONMENT, Element.CORE_ENVIRONMENT);
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case TRANSACTIONS_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        required.remove(element);
                        if (! encountered.add(element)) {
                            throw ParseUtils.unexpectedElement(reader);
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
                                throw ParseUtils.unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            if (! required.isEmpty()) {
                throw ParseUtils.missingRequiredElement(reader, required);
            }
        }

        static ModelNode parseObjectStoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode store = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case RELATIVE_TO:
                            store.get(RELATIVE_TO).set(value);
                            break;
                        case PATH:
                            store.get(PATH).set(value);
                            break;
                        default:
                            ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            return store;
        }

        static ModelNode parseCoordinatorEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode coordinator = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case ENABLE_STATISTICS:
                            coordinator.get(ENABLE_STATISTICS).set(value);
                            break;
                        case DEFAULT_TIMEOUT:
                            coordinator.get(DEFAULT_TIMEOUT).set(value);
                            break;
                        default:
                            ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            return coordinator;
        }

        static ModelNode parseCoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode env = new ModelNode();
            final int count = reader.getAttributeCount();
            final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    required.remove(attribute);
                    switch (attribute) {
                        case BINDING:
                            env.get(BINDING).set(value);
                            break;
                        case NODE_IDENTIFIER:
                            env.get(NODE_IDENTIFIER).set(value);
                            break;
                        case SOCKET_PROCESS_ID_MAX_PORTS:
                            env.get(SOCKET_PROCESS_ID_MAX_PORTS).set(value);
                            break;
                        default:
                            ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (! required.isEmpty()) {
                ParseUtils.missingRequired(reader, required);
            }
            // Handle elements
            ParseUtils.requireNoContent(reader);
            return env;
        }

        static ModelNode parseRecoveryEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode env = new ModelNode();
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case BINDING:
                            env.get(BINDING).set(value);
                            break;
                        case STATUS_BINDING:
                            env.get(STATUS_BINDING).set(value);
                            break;
                        default:
                            ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(! env.has(BINDING)) {
                ParseUtils.missingRequired(reader, Collections.singleton(Attribute.BINDING));
            }
            return env;
        }

    }

}
