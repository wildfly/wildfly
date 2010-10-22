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

package org.jboss.as.metadata.parser.jbossweb;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.RunAsMetaDataParser;
import org.jboss.as.metadata.parser.servlet.MultipartConfigMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.jboss.JBossAnnotationMetaData;

/**
 * @author Remy Maucherat
 */
public class JBossAnnotationMetaDataParser extends MetaDataElementParser {

    public static JBossAnnotationMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        JBossAnnotationMetaData annotation = new JBossAnnotationMetaData();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLASS_NAME:
                    annotation.setClassName(reader.getElementText());
                    break;
                case SERVLET_SECURITY:
                    annotation.setServletSecurity(ServletSecurityMetaDataParser.parse(reader));
                    break;
                case RUN_AS:
                    annotation.setRunAs(RunAsMetaDataParser.parse(reader));
                    break;
                case MULTIPART_CONFIG:
                    annotation.setMultipartConfig(MultipartConfigMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return annotation;
    }

}
