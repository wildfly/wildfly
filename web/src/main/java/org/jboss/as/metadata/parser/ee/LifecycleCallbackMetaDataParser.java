/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.metadata.parser.ee;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;

/**
 * @author Remy Maucherat
 */
public class LifecycleCallbackMetaDataParser extends MetaDataElementParser {

    public static LifecycleCallbackMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        LifecycleCallbackMetaData callback = new LifecycleCallbackMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LIFECYCLE_CALLBACK_CLASS:
                    callback.setClassName(reader.getElementText());
                    break;
                case LIFECYCLE_CALLBACK_METHOD:
                    callback.setMethodName(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }
        return callback;
    }

}
