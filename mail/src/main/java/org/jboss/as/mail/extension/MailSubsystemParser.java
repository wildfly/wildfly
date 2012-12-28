/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL;
import static org.jboss.as.mail.extension.MailServerDefinition.PASSWORD;
import static org.jboss.as.mail.extension.MailServerDefinition.SSL;
import static org.jboss.as.mail.extension.MailServerDefinition.TLS;
import static org.jboss.as.mail.extension.MailServerDefinition.USERNAME;
import static org.jboss.as.mail.extension.MailSessionDefinition.DEBUG;
import static org.jboss.as.mail.extension.MailSessionDefinition.FROM;
import static org.jboss.as.mail.extension.MailSessionDefinition.JNDI_NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.MAIL_SESSION;
import static org.jboss.as.mail.extension.MailSubsystemModel.NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The default subsystem parser / writer
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {


    /**
     * {@inheritDoc}
     */
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode model = context.getModelNode();
        if (model.hasDefined(MAIL_SESSION)) {
            List<Property> sessions = model.get(MAIL_SESSION).asPropertyList();

            for (Property mailSession : sessions) {
                ModelNode sessionData = mailSession.getValue();
                writer.writeStartElement(Element.MAIL_SESSION.getLocalName());

                JNDI_NAME.marshallAsAttribute(sessionData, writer);
                DEBUG.marshallAsAttribute(sessionData, false, writer);
                FROM.marshallAsAttribute(sessionData, false, writer);

                if (sessionData.hasDefined(SERVER_TYPE)) {
                    for (Property property : sessionData.get(SERVER_TYPE).asPropertyList()) {
                        String name = property.getName();
                        if (name.equals(SMTP)) {
                            writeServerModel(writer, property.getValue(), SMTP_SERVER, null);
                        } else if (name.equals(POP3)) {
                            writeServerModel(writer, property.getValue(), POP3_SERVER, null);
                        } else if (name.equals(IMAP)) {
                            writeServerModel(writer, property.getValue(), IMAP_SERVER, null);
                        } else {
                            throw new XMLStreamException("unknown model element " + name);
                        }
                    }
                }
                if (sessionData.hasDefined(CUSTOM)) {
                    for (Property property : sessionData.get(CUSTOM).asPropertyList()) {
                        String name = property.getName();
                        writeServerModel(writer, property.getValue(), CUSTOM_SERVER, name);
                    }
                }

                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeServerModel(final XMLExtendedStreamWriter writer, final ModelNode server, final String elementName, String name) throws XMLStreamException {

        boolean credentials = server.hasDefined(USER_NAME);
        final boolean properties = server.hasDefined(PROPERTIES);
        boolean shouldWriteEnd = false;
        if (credentials || properties) {
            writer.writeStartElement(Element.forName(elementName).getLocalName());
            shouldWriteEnd = true;
        } else {
            writer.writeEmptyElement(Element.forName(elementName).getLocalName());
        }
        if (name != null) {
            writer.writeAttribute(NAME, name);
        }
        SSL.marshallAsAttribute(server, false, writer);
        TLS.marshallAsAttribute(server, false, writer);
        OUTBOUND_SOCKET_BINDING_REF.marshallAsAttribute(server, false, writer);
        if (credentials) {
            writer.writeEmptyElement(Element.LOGIN.getLocalName());
            USERNAME.marshallAsAttribute(server, false, writer);
            PASSWORD.marshallAsAttribute(server, false, writer);

        }
        if (properties) {
            MailServerDefinition.PROPERTIES.marshallAsElement(server, writer);
        }
        if (shouldWriteEnd){
            writer.writeEndElement();
        }
    }


    /**
     * {@inheritDoc}
     */

    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH);
        list.add(Util.createAddOperation(address));

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0:
                case MAIL_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MAIL_SESSION: {
                            parseMailSession(reader, list, address);
                            break;
                        }
                        default: {
                            reader.handleAny(list);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseMailSession(final XMLExtendedStreamReader reader, List<ModelNode> list, final PathAddress parent) throws XMLStreamException {
        String jndiName = null;
        final ModelNode operation = new ModelNode();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.JNDI_NAME) {
                jndiName = value;
                JNDI_NAME.parseAndSetParameter(value, operation, reader);
            }
            if (attr == Attribute.DEBUG) {
                DEBUG.parseAndSetParameter(value, operation, reader);
            }
            if (attr == Attribute.FROM) {
                FROM.parseAndSetParameter(value, operation, reader);
            }
        }
        if (jndiName == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.JNDI_NAME));
        }
        final PathAddress address = parent.append(MAIL_SESSION, jndiName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operation.get(OP).set(ADD);
        list.add(operation);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0:
                case MAIL_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SMTP_SERVER: {
                            parseServerConfig(reader, SMTP, address, list);
                            break;
                        }
                        case POP3_SERVER: {
                            parseServerConfig(reader, POP3, address, list);
                            break;
                        }
                        case IMAP_SERVER: {
                            parseServerConfig(reader, IMAP, address, list);
                            break;
                        }
                        case CUSTOM_SERVER: {
                            parseCustomServerConfig(reader, address, list);
                            break;
                        }
                        default: {
                            reader.handleAny(list);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseServerConfig(final XMLExtendedStreamReader reader, final String name, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = parent.append(MailSubsystemModel.SERVER_TYPE, name);
        final ModelNode operation = Util.createAddOperation(address);
        list.add(operation);

        String socketBindingRef = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.OUTBOUND_SOCKET_BINDING_REF) {
                socketBindingRef = value;
                OUTBOUND_SOCKET_BINDING_REF.parseAndSetParameter(value, operation, reader);
            } else if (attr == Attribute.SSL) {
                SSL.parseAndSetParameter(value, operation, reader);
            } else if (attr == Attribute.TLS) {
                TLS.parseAndSetParameter(value, operation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (socketBindingRef == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.OUTBOUND_SOCKET_BINDING_REF));
        }
        parseLogin(reader, operation);
    }

    private void parseCustomServerConfig(final XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        final ModelNode operation = Util.createAddOperation(parent);
        list.add(operation);
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.OUTBOUND_SOCKET_BINDING_REF) {
                OUTBOUND_SOCKET_BINDING_REF_OPTIONAL.parseAndSetParameter(value, operation, reader);
            } else if (attr == Attribute.SSL) {
                SSL.parseAndSetParameter(value, operation, reader);
            } else if (attr == Attribute.TLS) {
                TLS.parseAndSetParameter(value, operation, reader);
            } else if (attr == Attribute.NAME) {
                name = value;
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        while (reader.nextTag() != END_ELEMENT) {
            if ("property".equals(reader.getLocalName())) {
                final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                MailServerDefinition.PROPERTIES.parseAndAddParameterElement(array[0], array[1], operation, reader);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }
        PathAddress address = parent.append(MailSubsystemModel.CUSTOM, name);
        operation.get(OP_ADDR).set(address.toModelNode());
        parseLogin(reader, operation);
    }

    private void parseLogin(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String att = reader.getAttributeLocalName(i);
                        String value = reader.getAttributeValue(i);
                        if (att.equals(Attribute.USERNAME.getLocalName())) {
                            MailServerDefinition.USERNAME.parseAndSetParameter(value, operation, reader);
                        } else if (att.equals(Attribute.PASSWORD.getLocalName())) {
                            PASSWORD.parseAndSetParameter(value, operation, reader);
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
            }
        }
    }
}
