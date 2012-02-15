package org.jboss.as.mail.extension;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailServerDefinition.PASSWORD;
import static org.jboss.as.mail.extension.MailServerDefinition.SSL;
import static org.jboss.as.mail.extension.MailServerDefinition.USERNAME;
import static org.jboss.as.mail.extension.MailSessionDefinition.DEBUG;
import static org.jboss.as.mail.extension.MailSessionDefinition.FROM;
import static org.jboss.as.mail.extension.MailSessionDefinition.JNDI_NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.MAIL_SESSION;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP_SERVER;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    private static final Logger log = Logger.getLogger(MailSubsystemParser.class);


    /**
     * {@inheritDoc}
     */
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        log.tracef("model node: %s", context.getModelNode());
        ModelNode model = context.getModelNode();
        List<Property> sessions = model.get(MAIL_SESSION).asPropertyList();

        for (Property mailSession : sessions) {
            String jndi = mailSession.getName();
            log.tracef("jndi: %s", jndi);
            ModelNode sessionData = mailSession.getValue();
            writer.writeStartElement(Element.MAIL_SESSION.getLocalName());

            JNDI_NAME.marshallAsAttribute(sessionData, writer);
            DEBUG.marshallAsAttribute(sessionData, false, writer);
            FROM.marshallAsAttribute(sessionData, false, writer);
            ModelNode server = sessionData.get(SERVER_TYPE);
            if (server.hasDefined(SMTP)) {
                writeServerModel(writer, server.get(SMTP), SMTP_SERVER);
            }
            if (server.hasDefined(POP3)) {
                writeServerModel(writer, server.get(POP3), POP3_SERVER);
            }
            if (server.hasDefined(IMAP)) {
                writeServerModel(writer, server.get(IMAP), IMAP_SERVER);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeServerModel(final XMLExtendedStreamWriter writer, final ModelNode server, final String elementName) throws XMLStreamException {

        boolean credentials = server.hasDefined(USER_NAME);
        if (credentials) {
            writer.writeStartElement(Element.forName(elementName).getLocalName());
        } else {
            writer.writeEmptyElement(Element.forName(elementName).getLocalName());
        }
        SSL.marshallAsAttribute(server, false, writer);
        OUTBOUND_SOCKET_BINDING_REF.marshallAsAttribute(server, false, writer);
        if (credentials) {
            writer.writeEmptyElement(Element.LOGIN.getLocalName());
            USERNAME.marshallAsAttribute(server, false, writer);
            PASSWORD.marshallAsAttribute(server, false, writer);
            writer.writeEndElement();
        }
    }


    /**
     * {@inheritDoc}
     */

    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, MailExtension.SUBSYSTEM_NAME);
        address.protect();
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
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
        log.tracef("list is: %s", list);
    }

    private void parseMailSession(final XMLExtendedStreamReader reader, List<ModelNode> list, final ModelNode parent) throws XMLStreamException {
        String jndiName = null;
        final ModelNode operation = new ModelNode();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.JNDI_NAME) {
                log.tracef("jndi name: %s", value);
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
        final ModelNode dsAddress = parent.clone();
        dsAddress.add(MAIL_SESSION, jndiName);
        dsAddress.protect();
        operation.get(OP_ADDR).set(dsAddress);
        operation.get(OP).set(ADD);
        list.add(operation);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SMTP_SERVER: {
                            parseServerConfig(reader, SMTP, dsAddress, list);
                            break;
                        }
                        case POP3_SERVER: {
                            parseServerConfig(reader, POP3, dsAddress, list);
                            break;
                        }
                        case IMAP_SERVER: {
                            parseServerConfig(reader, IMAP, dsAddress, list);
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

    private void parseServerConfig(final XMLExtendedStreamReader reader, final String name, final ModelNode parent, List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = parent.clone();
        address.add(MailSubsystemModel.SERVER_TYPE, name);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ADD);
        list.add(operation);

        String socketBindingRef = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.OUTBOUND_SOCKET_BINDING_REF) {
                socketBindingRef = value;
                OUTBOUND_SOCKET_BINDING_REF.parseAndSetParameter(value, operation, reader);
            }
            if (attr == Attribute.SSL) {
                SSL.parseAndSetParameter(value, operation, reader);
            }
        }
        if (socketBindingRef == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.OUTBOUND_SOCKET_BINDING_REF));
        }
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
