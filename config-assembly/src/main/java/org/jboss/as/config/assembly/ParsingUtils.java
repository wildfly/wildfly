/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ParsingUtils {
    static String getNextElement(XMLStreamReader reader, String name, Map<String, String> attributes, boolean getElementText) throws XMLStreamException {
        if (!reader.hasNext()) {
            throw new XMLStreamException("Expected more elements", reader.getLocation());
        }
        int type = reader.next();
        while (reader.hasNext() && type != START_ELEMENT) {
            type = reader.next();
        }
        if (reader.getEventType() != START_ELEMENT) {
            throw new XMLStreamException("No <" + name + "> found");
        }
        if (!reader.getLocalName().equals("" + name + "")) {
            throw new XMLStreamException("<" + name + "> expected", reader.getLocation());
        }

        if (attributes != null) {
            for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                String attr = reader.getAttributeLocalName(i);
                if (!attributes.containsKey(attr)) {
                    throw new XMLStreamException("Unexpected attribute " + attr, reader.getLocation());
                }
                attributes.put(attr, reader.getAttributeValue(i));
            }
        }

        return getElementText ? reader.getElementText() : null;
    }
}
