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

import org.jboss.as.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.as.metadata.parser.ee.EnvironmentRefsGroupMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentRefsGroupMetaData;
import org.jboss.metadata.web.spec.WebFragment30MetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;


/**
 * @author Remy Maucherat
 */
public class WebFragmentMetaDataParser extends MetaDataElementParser {

    public static WebFragmentMetaData parse(XMLStreamReader reader) throws XMLStreamException {

        reader.require(START_DOCUMENT, null, null);
        // Read until the first start element
        while (reader.hasNext() && reader.next() != START_ELEMENT) {
            // checkstyle
        }

        WebFragmentMetaData wmd = new WebFragment30MetaData();

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
                    wmd.setId(value);
                    break;
                }
                case VERSION: {
                    wmd.setVersion(value);
                    break;
                }
                case METADATA_COMPLETE: {
                    if (Boolean.TRUE.equals(Boolean.valueOf(value))) {
                        wmd.setMetadataComplete(true);
                    }
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        EnvironmentRefsGroupMetaData env = new EnvironmentRefsGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (WebCommonMetaDataParser.parse(reader, wmd)) {
                continue;
            }
            if (EnvironmentRefsGroupMetaDataParser.parse(reader, env)) {
                if (wmd.getJndiEnvironmentRefsGroup() == null) {
                    wmd.setJndiEnvironmentRefsGroup(env);
                }
                continue;
            }
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (wmd.getDescriptionGroup() == null) {
                    wmd.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    wmd.setName(reader.getElementText());
                    break;
                case ORDERING:
                    wmd.setOrdering(OrderingMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return wmd;
    }

}
