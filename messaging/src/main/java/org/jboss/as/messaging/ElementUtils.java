package org.jboss.as.messaging;

import org.hornetq.api.core.TransportConfiguration;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class ElementUtils {
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

   static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered", reader.getLocation());
    }
   static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }
   static TransportConfiguration parseTransportConfiguration(final XMLExtendedStreamReader reader, String name, Element parentElement)
      throws XMLStreamException {

      Map<String, Object> params = new HashMap<String, Object>();

      int tag = reader.getEventType();
      String localName = null;
      String clazz = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         Element element = Element.forName(localName);
         if(localName.equals(parentElement.getLocalName()) == true)
            break;

         switch(element) {
         case FACTORY_CLASS:
            clazz = reader.getElementText();
         break;
         case PARAM:
            int count = reader.getAttributeCount();
            String key = null, value = null;
            for(int n = 0; n < count; n ++) {
               String attrName = reader.getAttributeLocalName(n);
               Attribute attribute = Attribute.forName(attrName);
               switch (attribute) {
               case KEY:
                  key = reader.getAttributeValue(n);
                  break;
               case VALUE:
                  value = reader.getAttributeValue(n);
                  break;
               default:
                  throw unexpectedAttribute(reader, n);
               }
            }
            reader.discardRemainder();
            params.put(key, value);
            break;
         }
         // Scan to element end
      } while(reader.hasNext());

      return new TransportConfiguration(clazz, params, name);
   }
}
