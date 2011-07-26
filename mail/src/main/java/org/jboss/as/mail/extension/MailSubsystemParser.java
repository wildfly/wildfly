package org.jboss.as.mail.extension;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
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
            log.info("operation: "+operation);

        }

        log.info("parsing done, config is: " + sessionConfigList);
        log.info("list is: "+list);

    }


    private MailSessionConfig parseMailSession(final XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        log.info("parsing mail session");
        MailSessionConfig cfg = new MailSessionConfig();


        String jndiName = ParseUtils.requireAttributes(reader, "jndi-name")[0]; //todo add optional parameters
        log.trace("jndi name: " + jndiName);
        cfg.setJndiName(jndiName);


        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case MAIL_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGIN: {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                String att = reader.getAttributeLocalName(i);
                                if (att.equals("name")) {
                                    cfg.setUsername(reader.getAttributeValue(i));
                                } else if (att.equals("password")) {
                                    cfg.setPassword(reader.getAttributeValue(i));
                                }
                            }
                            ParseUtils.requireNoContent(reader);
                            break;
                        }
                        case SMTP_SERVER: {
                            String[] attributes = ParseUtils.requireAttributes(reader, "address", "port");
                            cfg.setSmtpServerAddress(attributes[0]);
                            cfg.setSmtpServerPort(attributes[1]);
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
