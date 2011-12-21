package org.jboss.as.mail.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

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
        List<Property> sessions = model.get(ModelKeys.MAIL_SESSION).asPropertyList();

        /*List<Property> props = mailSession.getValue().asPropertyList();
        log.info("properties: "+props);*/
        for (Property mailSession : sessions) {
            String jndi = mailSession.getName();
            log.tracef("jndi: %s", jndi);
            ModelNode sessionData = mailSession.getValue();
            writer.writeStartElement(Element.MAIL_SESSION.getLocalName());

            writer.writeAttribute(Attribute.JNDI_NAME.getLocalName(), jndi);
            if (sessionData.hasDefined(ModelKeys.DEBUG)) {
                writer.writeAttribute(Attribute.DEBUG.getLocalName(), sessionData.get(ModelKeys.DEBUG).asString());
            }


            if (sessionData.hasDefined(ModelKeys.SMTP_SERVER)) {
                writeServerModel(writer, sessionData, ModelKeys.SMTP_SERVER);

            }
            if (sessionData.hasDefined(ModelKeys.POP3_SERVER)) {
                writeServerModel(writer, sessionData, ModelKeys.POP3_SERVER);
            }

            if (sessionData.hasDefined(ModelKeys.IMAP_SERVER)) {
                writeServerModel(writer, sessionData, ModelKeys.IMAP_SERVER);
            }


            //writer.writeEndElement();

            writer.writeEndElement();
        }

        writer.writeEndElement();

    }

    private void writeServerModel(XMLExtendedStreamWriter writer, ModelNode sessionData, final String name) throws XMLStreamException {
        ModelNode server = sessionData.get(name);
        boolean credentials = server.hasDefined(ModelKeys.CREDENTIALS);
        if (credentials) {
            writer.writeStartElement(Element.forName(name).getLocalName());
        } else {
            writer.writeEmptyElement(Element.forName(name).getLocalName());
        }
        writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName(), server.get(ModelKeys.OUTBOUND_SOCKET_BINDING_REF).asString());
        if (credentials) {
            writer.writeEmptyElement(Element.LOGIN.getLocalName());
            writer.writeAttribute(Attribute.USERNAME.getLocalName(), server.get(ModelKeys.CREDENTIALS, ModelKeys.USERNAME).asString());
            writer.writeAttribute(Attribute.PASSWORD.getLocalName(), server.get(ModelKeys.CREDENTIALS, ModelKeys.PASSWORD).asString());
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


        List<MailSessionConfig> sessionConfigList = new LinkedList<MailSessionConfig>();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MAIL_SESSION: {
                            parseMailSession(reader, address, list);
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

//        for (MailSessionConfig c : sessionConfigList) {
//            final ModelNode dsAddress = address.clone();
//            dsAddress.add(ModelKeys.MAIL_SESSION, c.getJndiName());
//            dsAddress.protect();
//
//            final ModelNode operation = new ModelNode();
//            operation.get(OP_ADDR).set(dsAddress);
//            operation.get(OP).set(ADD);
//
//            Util.fillFrom(operation, c);
//            list.add(operation);
//
//
//        }

        log.tracef("parsing done, config is: %s", sessionConfigList);
        log.tracef("list is: %s", list);

    }


    private void parseMailSession(final XMLExtendedStreamReader reader, ModelNode subsystemAddr, List<ModelNode> list) throws XMLStreamException {
        log.debug("parsing mail session");


        String jndiName = null;
        Boolean debug = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attr = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            if (attr == Attribute.JNDI_NAME) {
                jndiName = value;
                log.tracef("jndi name: %s", jndiName);
            }
            if (attr == Attribute.DEBUG) {
                debug = Boolean.parseBoolean(value.trim());
            }
        }

        if (jndiName == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.JNDI_NAME));
        }

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(subsystemAddr.clone()).add(ModelKeys.MAIL_SESSION, jndiName);
        if (debug != null) {
            op.get(ModelKeys.DEBUG).set(debug);
        }
        op.get(ModelKeys.JNDI_NAME).set(jndiName);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SMTP_SERVER: {
                            op.get(ModelKeys.SMTP_SERVER).set(parseServerConfig(reader));
                            break;
                        }
                        case POP3_SERVER: {
                            op.get(ModelKeys.POP3_SERVER).set(parseServerConfig(reader));
                            break;
                        }
                        case IMAP_SERVER: {
                            op.get(ModelKeys.IMAP_SERVER).set(parseServerConfig(reader));
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
        list.add(op);
    }

    private ModelNode parseServerConfig(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode svrCfg = new ModelNode();
        svrCfg.get(ModelKeys.OUTBOUND_SOCKET_BINDING_REF).set(ParseUtils.requireAttributes(reader, Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName())[0]);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGIN: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String att = reader.getAttributeLocalName(i);
                        if (att.equals(Attribute.USERNAME.getLocalName())) {
                            svrCfg.get(ModelKeys.CREDENTIALS, ModelKeys.USERNAME).set(reader.getAttributeValue(i));
                        } else if (att.equals(Attribute.PASSWORD.getLocalName())) {
                            svrCfg.get(ModelKeys.CREDENTIALS, ModelKeys.PASSWORD).set(reader.getAttributeValue(i));
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
            }
        }
        return svrCfg;
    }
}
