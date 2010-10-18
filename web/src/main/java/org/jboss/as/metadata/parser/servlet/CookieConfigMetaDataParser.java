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

package org.jboss.as.metadata.parser.servlet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.spec.CookieConfigMetaData;

/**
 * @author Remy Maucherat
 */
public class CookieConfigMetaDataParser extends MetaDataElementParser {

    public static CookieConfigMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        CookieConfigMetaData cookieConfig = new CookieConfigMetaData();

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
                    cookieConfig.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    cookieConfig.setName(reader.getElementText());
                    break;
                case DOMAIN:
                    cookieConfig.setDomain(reader.getElementText());
                    break;
                case PATH:
                    cookieConfig.setPath(reader.getElementText());
                    break;
                case COMMENT:
                    cookieConfig.setComment(reader.getElementText());
                    break;
                case HTTP_ONLY:
                    if (Boolean.TRUE.equals(Boolean.valueOf(reader.getElementText()))) {
                        cookieConfig.setHttpOnly(true);
                    } else {
                        cookieConfig.setHttpOnly(false);
                    }
                    break;
                case SECURE:
                    if (Boolean.TRUE.equals(Boolean.valueOf(reader.getElementText()))) {
                        cookieConfig.setSecure(true);
                    } else {
                        cookieConfig.setSecure(false);
                    }
                    break;
                case MAX_AGE:
                    try {
                        cookieConfig.setMaxAge(Integer.valueOf(reader.getElementText()));
                    } catch (NumberFormatException e) {
                        throw unexpectedValue(reader, e);
                    }
                    break;
                 default: throw unexpectedElement(reader);
            }
        }

        return cookieConfig;
    }

}
