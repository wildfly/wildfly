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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;

/**
 * @author Remy Maucherat
 */
public class JspPropertyGroupMetaDataParser extends MetaDataElementParser {

    public static JspPropertyGroupMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        JspPropertyGroupMetaData jspPropertyGroup = new JspPropertyGroupMetaData();

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
                    jspPropertyGroup.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (jspPropertyGroup.getDescriptionGroup() == null) {
                    jspPropertyGroup.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case URL_PATTERN:
                    List<String> urlPatterns = jspPropertyGroup.getUrlPatterns();
                    if (urlPatterns == null) {
                        urlPatterns = new ArrayList<String>();
                        jspPropertyGroup.setUrlPatterns(urlPatterns);
                    }
                    urlPatterns.add(reader.getElementText());
                    break;
                case EL_IGNORED:
                    jspPropertyGroup.setElIgnored(reader.getElementText());
                    break;
                case PAGE_ENCODING:
                    jspPropertyGroup.setPageEncoding(reader.getElementText());
                    break;
                case SCRIPTING_INVALID:
                    jspPropertyGroup.setScriptingInvalid(reader.getElementText());
                    break;
                case IS_XML:
                    jspPropertyGroup.setIsXml(reader.getElementText());
                    break;
                case INCLUDE_PRELUDE:
                    List<String> includePreludes = jspPropertyGroup.getIncludePreludes();
                    if (includePreludes == null) {
                        includePreludes = new ArrayList<String>();
                        jspPropertyGroup.setIncludePreludes(includePreludes);
                    }
                    includePreludes.add(reader.getElementText());
                    break;
                case INCLUDE_CODA:
                    List<String> includeCodas = jspPropertyGroup.getIncludeCodas();
                    if (includeCodas == null) {
                        includeCodas = new ArrayList<String>();
                        jspPropertyGroup.setIncludeCodas(includeCodas);
                    }
                    includeCodas.add(reader.getElementText());
                    break;
                case DEFERRED_SYNTAX_ALLOWED_AS_LITERAL:
                    jspPropertyGroup.setDeferredSyntaxAllowedAsLiteral(reader.getElementText());
                    break;
                case TRIM_DIRECTIVE_WHITESPACES:
                    jspPropertyGroup.setTrimDirectiveWhitespaces(reader.getElementText());
                    break;
                case DEFAULT_CONTENT_TYPE:
                    jspPropertyGroup.setDefaultContentType(reader.getElementText());
                    break;
                case BUFFER:
                    jspPropertyGroup.setBuffer(reader.getElementText());
                    break;
                case ERROR_ON_UNDECLARED_NAMESPACE:
                    jspPropertyGroup.setErrorOnUndeclaredNamespace(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return jspPropertyGroup;
    }

}
