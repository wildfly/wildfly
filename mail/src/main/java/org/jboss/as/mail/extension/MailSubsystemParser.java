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
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL;
import static org.jboss.as.mail.extension.MailServerDefinition.PASSWORD;
import static org.jboss.as.mail.extension.MailServerDefinition.SSL;
import static org.jboss.as.mail.extension.MailServerDefinition.TLS;
import static org.jboss.as.mail.extension.MailSessionDefinition.DEBUG;
import static org.jboss.as.mail.extension.MailSessionDefinition.FROM;
import static org.jboss.as.mail.extension.MailSessionDefinition.JNDI_NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.LOGIN;
import static org.jboss.as.mail.extension.MailSubsystemModel.MAIL_SESSION;
import static org.jboss.as.mail.extension.MailSubsystemModel.NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.PROPERTY;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;

import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The 1.x subsystem parser / writer
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    MailSubsystemParser() {

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
                case MAIL_1_1:
                case MAIL_1_2: {
                    final String element = reader.getLocalName();
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
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            switch (attr) {
                case MailSubsystemModel.JNDI_NAME:
                    jndiName = value;
                    JNDI_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.DEBUG:
                    DEBUG.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.FROM:
                    FROM.parseAndSetParameter(value, operation, reader);
                    break;
            }
        }
        if (jndiName == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(JNDI_NAME));
        }
        final PathAddress address = parent.append(MAIL_SESSION, jndiName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operation.get(OP).set(ADD);
        list.add(operation);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0:
                case MAIL_1_1:
                case MAIL_1_2: {
                    final String element = reader.getLocalName();
                    switch (element) {
                        case MailSubsystemModel.SMTP_SERVER: {
                            parseServerConfig(reader, SMTP, address, list);
                            break;
                        }
                        case MailSubsystemModel.POP3_SERVER: {
                            parseServerConfig(reader, POP3, address, list);
                            break;
                        }
                        case MailSubsystemModel.IMAP_SERVER: {
                            parseServerConfig(reader, IMAP, address, list);
                            break;
                        }
                        case MailSubsystemModel.CUSTOM_SERVER: {
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
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            switch (attr) {
                case MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF:
                    socketBindingRef = value;
                    OUTBOUND_SOCKET_BINDING_REF.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.SSL:
                    SSL.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.TLS:
                    TLS.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (socketBindingRef == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(OUTBOUND_SOCKET_BINDING_REF));
        }
        parseLogin(reader, operation);
    }

    private void parseCustomServerConfig(final XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        final ModelNode operation = Util.createAddOperation(parent);
        list.add(operation);
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            switch (attr) {
                case MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF:
                    OUTBOUND_SOCKET_BINDING_REF_OPTIONAL.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.SSL:
                    SSL.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.TLS:
                    TLS.parseAndSetParameter(value, operation, reader);
                    break;
                case MailSubsystemModel.NAME:
                    name = value;
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String element = reader.getLocalName();
            switch (element) {
                case LOGIN: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String att = reader.getAttributeLocalName(i);
                        String value = reader.getAttributeValue(i);
                        if (att.equals(MailSubsystemModel.USER_NAME)) {
                            MailServerDefinition.USERNAME.parseAndSetParameter(value, operation, reader);
                        } else if (att.equals(MailSubsystemModel.PASSWORD)) {
                            PASSWORD.parseAndSetParameter(value, operation, reader);
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case PROPERTY: {
                    final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                    MailServerDefinition.PROPERTIES.parseAndAddParameterElement(array[0], array[1], operation, reader);
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }


        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(NAME));
        }
        PathAddress address = parent.append(MailSubsystemModel.CUSTOM, name);
        operation.get(OP_ADDR).set(address.toModelNode());
    }

    private void parseLogin(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String element = reader.getLocalName();
            switch (element) {
                case LOGIN: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String att = reader.getAttributeLocalName(i);
                        String value = reader.getAttributeValue(i);
                        if (att.equals(MailSubsystemModel.USER_NAME)) {
                            MailServerDefinition.USERNAME.parseAndSetParameter(value, operation, reader);
                        } else if (att.equals(MailSubsystemModel.PASSWORD)) {
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
