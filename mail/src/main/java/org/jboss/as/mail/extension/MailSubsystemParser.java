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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.mail.extension.MailSubsystemModel.*;

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
        List<Property> sessions = model.get(MailSubsystemModel.MAIL_SESSION).asPropertyList();

        for (Property mailSession : sessions) {
            String jndi = mailSession.getName();
            log.tracef("jndi: %s", jndi);
            ModelNode sessionData = mailSession.getValue();
            writer.writeStartElement(Element.MAIL_SESSION.getLocalName());

            writer.writeAttribute(Attribute.JNDI_NAME.getLocalName(), jndi);
            if (sessionData.hasDefined(DEBUG)) {
                writer.writeAttribute(Attribute.DEBUG.getLocalName(), sessionData.get(MailSubsystemModel.DEBUG).asString());
            }
            if (sessionData.hasDefined(MailSubsystemModel.FROM)) {
                writer.writeAttribute(Attribute.FROM.getLocalName(), sessionData.get(MailSubsystemModel.FROM).asString());
            }
            ModelNode server = sessionData.get(SERVER_TYPE);
            if (server.hasDefined(SMTP)) {
                writeServerModel(writer, server, SMTP, SMTP_SERVER);
            }
            if (server.hasDefined(POP3)) {
                writeServerModel(writer, server, POP3, POP3_SERVER);
            }
            if (server.hasDefined(IMAP)) {
                writeServerModel(writer, server, IMAP, IMAP_SERVER);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeServerModel(XMLExtendedStreamWriter writer, ModelNode sessionData, final String name, final String elementName) throws XMLStreamException {
        ModelNode server = sessionData.get(name);
        boolean credentials = server.hasDefined(MailSubsystemModel.USER_NAME);
        if (credentials) {
            writer.writeStartElement(Element.forName(elementName).getLocalName());
        } else {
            writer.writeEmptyElement(Element.forName(elementName).getLocalName());
        }
        boolean sslDefined = server.get(MailSubsystemModel.SSL).asBoolean(false);
        if (sslDefined) {
            writer.writeAttribute(Attribute.SSL.getLocalName(), server.get(MailSubsystemModel.SSL).asString());
        }
        writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName(), server.get(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF).asString());
        if (credentials) {
            writer.writeEmptyElement(Element.LOGIN.getLocalName());
            writer.writeAttribute(Attribute.USERNAME.getLocalName(), server.get(MailSubsystemModel.USER_NAME).asString());
            writer.writeAttribute(Attribute.PASSWORD.getLocalName(), server.get(MailSubsystemModel.PASSWORD).asString());
            writer.writeEndElement();
        }
    }


    /**
     * {@inheritDoc}
     */

    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        List<MailSessionConfig> sessionConfigList = new LinkedList<MailSessionConfig>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MAIL_SESSION: {
                            sessionConfigList.add(parseMailSession(reader, list));
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

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, MailExtension.SUBSYSTEM_NAME);
        address.protect();


        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);

        list.add(subsystem);

        for (MailSessionConfig c : sessionConfigList) {
            final ModelNode dsAddress = address.clone();
            dsAddress.add(MailSubsystemModel.MAIL_SESSION, c.getJndiName());
            dsAddress.protect();

            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(dsAddress);
            operation.get(OP).set(ADD);
            operation.get(JNDI_NAME).set(c.getJndiName());
            operation.get(DEBUG).set(c.isDebug());
            if (c.getFrom() != null) {
                operation.get(FROM).set(c.getFrom());
            }
            list.add(operation);
            fillSessionData(c, dsAddress, list);
        }
        log.tracef("parsing done, config is: %s", sessionConfigList);
        log.tracef("list is: %s", list);

    }

    static void fillSessionData(final MailSessionConfig sessionConfig, final ModelNode address, List<ModelNode> list) {
        if (sessionConfig.getSmtpServer() != null) {
            Util.addServerConfig(sessionConfig.getSmtpServer(), SMTP, address, list);
        }
        if (sessionConfig.getPop3Server() != null) {
            Util.addServerConfig(sessionConfig.getPop3Server(), POP3, address, list);
        }
        if (sessionConfig.getImapServer() != null) {
            Util.addServerConfig(sessionConfig.getImapServer(), IMAP, address, list);
        }
    }


    private MailSessionConfig parseMailSession(final XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        log.debug("parsing mail session");
        MailSessionConfig cfg = new MailSessionConfig();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.JNDI_NAME) {
                log.tracef("jndi name: %s", value);
                cfg.setJndiName(value);
            }
            if (attr == Attribute.DEBUG) {
                boolean debug = Boolean.parseBoolean(value.trim());
                cfg.setDebug(debug);
            }
            if (attr == Attribute.FROM) {
                cfg.setFrom(value);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SMTP_SERVER: {
                            cfg.setSmtpServer(parseServerConfig(reader));
                            break;
                        }
                        case POP3_SERVER: {
                            cfg.setPop3Server(parseServerConfig(reader));
                            break;
                        }
                        case IMAP_SERVER: {
                            cfg.setImapServer(parseServerConfig(reader));
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
        return cfg;
    }

    private MailSessionServer parseServerConfig(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String socketBindingRef = null;
        String username = null;
        String password = null;
        boolean ssl = false;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.OUTBOUND_SOCKET_BINDING_REF) {
                socketBindingRef = value;
            }
            if (attr == Attribute.SSL) {
                ssl = Boolean.parseBoolean(value.trim());

            }
        }
        if (socketBindingRef == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName()));
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String att = reader.getAttributeLocalName(i);
                        if (att.equals(Attribute.USERNAME.getLocalName())) {
                            username = reader.getAttributeValue(i);
                        } else if (att.equals(Attribute.PASSWORD.getLocalName())) {
                            password = reader.getAttributeValue(i);
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
            }
        }
        return new MailSessionServer(socketBindingRef, username, password, ssl);
    }
}
