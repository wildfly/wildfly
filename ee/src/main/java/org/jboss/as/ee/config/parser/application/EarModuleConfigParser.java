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

package org.jboss.as.ee.config.parser.application;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.metadata.ear.spec.ConnectorModuleMetaData;
import org.jboss.metadata.ear.spec.EjbModuleMetaData;
import org.jboss.metadata.ear.spec.JavaModuleMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.WebModuleMetaData;
import org.jboss.metadata.parser.util.MetaDataElementParser;

/**
 * @author John Bailey
 */
public class EarModuleConfigParser extends MetaDataElementParser {

    public static ModuleMetaData parse(final XMLStreamReader reader) throws XMLStreamException {
        final ModuleMetaData moduleMetaData = new ModuleMetaData();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    moduleMetaData.setId(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR:
                    moduleMetaData.setValue(parseConnector(reader));
                    break;
                case EJB:
                    moduleMetaData.setValue(parseEjb(reader));
                    break;
                case JAVA:
                    moduleMetaData.setValue(parseJava(reader));
                    break;
                case WEB:
                    moduleMetaData.setValue(parseWeb(reader));
                    break;
                case ALT_DD:
                    moduleMetaData.setAlternativeDD(reader.getElementText());
                default:
                    throw unexpectedElement(reader);
            }
        }


        return moduleMetaData;
    }

    private static ConnectorModuleMetaData parseConnector(final XMLStreamReader reader) throws XMLStreamException {
        final ConnectorModuleMetaData module = new ConnectorModuleMetaData();
        module.setFileName(reader.getElementText());
        return module;
    }

    private static EjbModuleMetaData parseEjb(final XMLStreamReader reader) throws XMLStreamException {
        final EjbModuleMetaData module = new EjbModuleMetaData();
        module.setFileName(reader.getElementText());
        return module;
    }

    private static JavaModuleMetaData parseJava(final XMLStreamReader reader) throws XMLStreamException {
        final JavaModuleMetaData module = new JavaModuleMetaData();
        module.setFileName(reader.getElementText());
        return module;
    }

    private static WebModuleMetaData parseWeb(final XMLStreamReader reader) throws XMLStreamException {
        final WebModuleMetaData webModuleMetaData = new WebModuleMetaData();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    webModuleMetaData.setId(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONTEXT_ROOT:
                    webModuleMetaData.setContextRoot(reader.getElementText());
                    break;
                case WEB_URI:
                    webModuleMetaData.setWebURI(reader.getElementText());
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        return webModuleMetaData;
    }
}
