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
import static org.jboss.as.connector.subsystems.connector.NewConnectorSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.connector.NewConnectorSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.connector.NewConnectorSubsystemProviders.SUBSYSTEM_REMOVE_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.model.ParseUtils.missingRequired;
import static org.jboss.as.model.ParseUtils.missingRequiredElement;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class NewConnectorExtension implements NewExtension {

    @Override
    public void initialize(final NewExtensionContext context) {
        // Register the connector subsystem
        final SubsystemRegistration registration = context.registerSubsystem(CONNECTOR);

        registration.registerXMLElementWriter(NewConnectorSubsystemParser.INSTANCE);

        // Connector subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler("add", NewConnectorSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler("remove", NewConnectorSubSystemRemove.INSTANCE, SUBSYSTEM_REMOVE_DESC, false);

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewConnectorSubsystemParser.INSTANCE);
    }

    static final class NewConnectorSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final NewConnectorSubsystemParser INSTANCE = new NewConnectorSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();
            // FIXME write out the details
            writer.writeEndElement();
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, CONNECTOR);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // Handle elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case CONNECTOR_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (!visited.add(element)) {
                            throw unexpectedElement(reader);
                        }
                        final EnumSet<Element> requiredElement = EnumSet.of(Element.DEFAULT_WORKMANAGER);

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
                        if (!requiredElement.isEmpty()) {
                            missingRequiredElement(reader, requiredElement);
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
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

        }

        private void parseBeanValidation(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {

            final boolean enabled = ParseUtils.readBooleanAttributeElement(reader, Attribute.ENABLED.getLocalName());
            node.get(BEAN_VALIDATION_ENABLED).set(enabled);

        }
    }
}
