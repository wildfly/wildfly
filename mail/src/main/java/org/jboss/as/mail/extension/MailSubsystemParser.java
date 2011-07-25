package org.jboss.as.mail.extension;

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
import java.util.List;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
class MailSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    private static final Logger log = Logger.getLogger(MailSubsystemParser.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(MailSubsystemExtension.NAMESPACE, false);
        writer.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // Require no content
        //ParseUtils.requireNoContent(reader);
        list.add(MailSubsystemExtension.createAddSubsystemOperation());

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = reader.getLocalName();
            log.info("element: "+name);
            if ("mail-session".equals(name)){
                ParseUtils.requireAttributes(reader, "jndi-name");
                String jndiName = reader.getAttributeValue(0);
                log.info("jndi name: "+jndiName);
            }
            if ("username".equals(name)){
                String username = reader.getElementText();
                log.info("username: "+username);
            }
            if ("password".equals(name)){
                String pass= reader.getElementText();
                log.info("password: "+pass);
            }

            if ("properties".equals(name)){
                while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    name = reader.getLocalName();
                    ParseUtils.requireSingleAttribute(reader,"name");
                    log.info("name: "+name);
                    log.info("prop name: "+reader.getAttributeValue(0)+ " has value: "+reader.getElementText());
                }

            }

        }
        log.info("parsing done");
        reader.discardRemainder();
        //reader.handleAny(list);
        //list.add(sub)

    }
}
