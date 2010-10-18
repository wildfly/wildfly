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

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;

/**
 * @author Remy Maucherat
 */
public class FilterMappingMetaDataParser extends MetaDataElementParser {

    public static FilterMappingMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    filterMapping.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case FILTER_NAME:
                    filterMapping.setFilterName(reader.getElementText());
                    break;
                case URL_PATTERN:
                    List<String> urlPatterns = filterMapping.getUrlPatterns();
                    if (urlPatterns == null) {
                        urlPatterns = new ArrayList<String>();
                        filterMapping.setUrlPatterns(urlPatterns);
                    }
                    urlPatterns.add(reader.getElementText());
                    break;
                case SERVLET_NAME:
                    List<String> servletNames = filterMapping.getServletNames();
                    if (servletNames == null) {
                        servletNames = new ArrayList<String>();
                        filterMapping.setServletNames(servletNames);
                    }
                    servletNames.add(reader.getElementText());
                    break;
                case DISPATCHER:
                    List<DispatcherType> dispatchers = filterMapping.getDispatchers();
                    if (dispatchers == null) {
                        dispatchers = new ArrayList<DispatcherType>();
                        filterMapping.setDispatchers(dispatchers);
                    }
                    dispatchers.add(DispatcherType.valueOf(reader.getElementText()));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return filterMapping;
    }

}
