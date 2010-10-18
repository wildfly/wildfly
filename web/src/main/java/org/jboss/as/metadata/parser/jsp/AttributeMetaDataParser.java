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

package org.jboss.as.metadata.parser.jsp;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.DescriptionsMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.web.spec.AttributeMetaData;

/**
 * @author Remy Maucherat
 */
public class AttributeMetaDataParser extends MetaDataElementParser {

    public static AttributeMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        AttributeMetaData attributeMD = new AttributeMetaData();

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    attributeMD.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionsImpl descriptions = new DescriptionsImpl();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionsMetaDataParser.parse(reader, descriptions)) {
                if (attributeMD.getDescriptions() == null) {
                    attributeMD.setDescriptions(descriptions);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    attributeMD.setName(reader.getElementText());
                    break;
                case REQUIRED:
                    attributeMD.setRequired(reader.getElementText());
                    break;
                case RTEXPRVALUE:
                    attributeMD.setRtexprvalue(reader.getElementText());
                    break;
                case TYPE:
                    attributeMD.setType(reader.getElementText());
                    break;
                case FRAGMENT:
                    attributeMD.setFragment(reader.getElementText());
                    break;
                case DEFERRED_VALUE:
                    attributeMD.setDeferredValue(DeferredValueMetaDataParser.parse(reader));
                    break;
                case DEFERRED_METHOD:
                    attributeMD.setDeferredMethod(DeferredMethodMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return attributeMD;
    }

}
