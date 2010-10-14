package org.jboss.as.messaging;

import org.hornetq.api.core.SimpleString;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.logging.Logger;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
class ElementUtils {
    static Logger log = Logger.getLogger("org.jboss.as.messaging");

    enum StaxEvent {
        START_ELEMENT(1),
        END_ELEMENT(2),
        PROCESSING_INSTRUCTION(3),
        CHARACTERS(4),
        COMMENT(5),
        SPACE(6),
        START_DOCUMENT(7),
        END_DOCUMENT(8),
        ENTITY_REFERENCE(9),
        ATTRIBUTE(10),
        DTD(11),
        CDATA(12),
        NAMESPACE(13),
        NOTATION_DECLARATION(14),
        ENTITY_DECLARATION(15);

        /** Stash the values for use as an array indexed by StaxEvent.tag-1 */
        private static StaxEvent[] EVENTS = values();
        private final int tag;

        StaxEvent(int tag) {
            this.tag = tag;
        }

        static StaxEvent tagToEvent(int tag) {
            return EVENTS[tag - 1];
        }
    }

    static void writeSimpleElement(final Element element, final String content, final XMLExtendedStreamWriter streamWriter)
            throws XMLStreamException {
        if (content != null && content.length() > 0) {
            streamWriter.writeStartElement(element.getLocalName());
            streamWriter.writeCharacters(content);
            streamWriter.writeEndElement();
        }
    }

    static void writeSimpleElement(final Element element, final SimpleString content, final XMLExtendedStreamWriter streamWriter)
            throws XMLStreamException {
        if (content != null) {
            writeSimpleElement(element, content.toString(), streamWriter);
        }
    }
}
