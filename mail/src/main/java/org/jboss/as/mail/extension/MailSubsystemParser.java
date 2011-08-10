package org.jboss.as.mail.extension;

import com.sun.xml.internal.messaging.saaj.util.ParseUtil;
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
import java.util.LinkedList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

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
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        log.trace("model node: " + context.getModelNode());
        ModelNode model = context.getModelNode();
        List<Property> sessions = model.get(ModelKeys.MAIL_SESSION).asPropertyList();

        /*List<Property> props = mailSession.getValue().asPropertyList();
        log.info("properties: "+props);*/
        for (Property mailSession : sessions) {
            String jndi = mailSession.getName();
            log.info("jndi: " + jndi);
            ModelNode sessionData = mailSession.getValue();
            writer.writeStartElement(Element.MAIL_SESSION.getLocalName());

            writer.writeAttribute(Attribute.JNDI_NAME.getLocalName(), jndi);
            if (sessionData.hasDefined(ModelKeys.DEBUG)){
                writer.writeAttribute(Attribute.DEBUG.getLocalName(), sessionData.get(ModelKeys.DEBUG).asString());
            }
            writer.writeEmptyElement(Element.LOGIN.getLocalName());
            writer.writeAttribute(Attribute.USERNAME.getLocalName(), sessionData.get(ModelKeys.USERNAME).asString());
            writer.writeAttribute(Attribute.PASSWORD.getLocalName(), sessionData.get(ModelKeys.PASSWORD).asString());
            //writer.writeEndElement();

            if (sessionData.hasDefined(ModelKeys.SMTP_SERVER)) {
                ModelNode server = sessionData.get(ModelKeys.SMTP_SERVER);
                writer.writeEmptyElement(Element.SMTP_SERVER.getLocalName());
                writer.writeAttribute(Attribute.SERVER_ADDRESS.getLocalName(), server.get(ModelKeys.SERVER_ADDRESS).asString());
                writer.writeAttribute(Attribute.SERVER_PORT.getLocalName(), server.get(ModelKeys.SERVER_PORT).asString());
            }
            if (sessionData.hasDefined(ModelKeys.POP3_SERVER)) {
                ModelNode server = sessionData.get(ModelKeys.POP3_SERVER);
                writer.writeEmptyElement(Element.POP3_SERVER.getLocalName());
                writer.writeAttribute(Attribute.SERVER_ADDRESS.getLocalName(), server.get(ModelKeys.SERVER_ADDRESS).asString());
                writer.writeAttribute(Attribute.SERVER_PORT.getLocalName(), server.get(ModelKeys.SERVER_PORT).asString());
            }

            if (sessionData.hasDefined(ModelKeys.IMAP_SERVER)) {
                ModelNode server = sessionData.get(ModelKeys.IMAP_SERVER);
                writer.writeEmptyElement(Element.IMAP_SERVER.getLocalName());
                writer.writeAttribute(Attribute.SERVER_ADDRESS.getLocalName(), server.get(ModelKeys.SERVER_ADDRESS).asString());
                writer.writeAttribute(Attribute.SERVER_PORT.getLocalName(), server.get(ModelKeys.SERVER_PORT).asString());
            }


            //writer.writeEndElement();

            writer.writeEndElement();
        }

        writer.writeEndElement();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, MailSubsystemExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);

        list.add(subsystem);


        List<MailSessionConfig> sessionConfigList = new LinkedList<MailSessionConfig>();
        /*
       <mail-session jndi-name="java:/Mail">
           <login name="nobody" password="pass"/>
           <smtp-server address="localhost" port="9999"/>
      </mail-session>
        */

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

        for (MailSessionConfig c : sessionConfigList) {
            final ModelNode dsAddress = address.clone();
            dsAddress.add(ModelKeys.MAIL_SESSION, c.getJndiName());
            dsAddress.protect();

            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(dsAddress);
            operation.get(OP).set(ADD);

            Util.fillFrom(operation, c);
            list.add(operation);


        }

        log.trace("parsing done, config is: " + sessionConfigList);
        log.trace("list is: " + list);

    }


    private MailSessionConfig parseMailSession(final XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        log.info("parsing mail session");
        MailSessionConfig cfg = new MailSessionConfig();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.JNDI_NAME) {
                String jndiName = value;
                log.trace("jndi name: " + jndiName);
                cfg.setJndiName(jndiName);
            }
            if (attr == Attribute.DEBUG) {
                boolean debug = Boolean.parseBoolean(value.trim());
                cfg.setDebug(debug);
            }

        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {


                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGIN: {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                String att = reader.getAttributeLocalName(i);
                                if (att.equals(Attribute.USERNAME.getLocalName())) {
                                    cfg.setUsername(reader.getAttributeValue(i));
                                } else if (att.equals(Attribute.PASSWORD.getLocalName())) {
                                    cfg.setPassword(reader.getAttributeValue(i));
                                }
                            }
                            ParseUtils.requireNoContent(reader);
                            break;
                        }
                        case SMTP_SERVER: {
                            String[] attributes = ParseUtils.requireAttributes(reader, Attribute.SERVER_ADDRESS.getLocalName(), Attribute.SERVER_PORT.getLocalName());
                            cfg.setSmtpServer(new MailSessionServer(attributes[0], attributes[1]));
                            ParseUtils.requireNoContent(reader);
                            break;
                        }
                        case POP3_SERVER: {
                            String[] attributes = ParseUtils.requireAttributes(reader, Attribute.SERVER_ADDRESS.getLocalName(), Attribute.SERVER_PORT.getLocalName());
                            cfg.setPop3Server(new MailSessionServer(attributes[0], attributes[1]));
                            ParseUtils.requireNoContent(reader);
                            break;
                        }
                        case IMAP_SERVER: {
                            String[] attributes = ParseUtils.requireAttributes(reader, Attribute.SERVER_ADDRESS.getLocalName(), Attribute.SERVER_PORT.getLocalName());
                            cfg.setImapServer(new MailSessionServer(attributes[0], attributes[1]));
                            ParseUtils.requireNoContent(reader);
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

}
