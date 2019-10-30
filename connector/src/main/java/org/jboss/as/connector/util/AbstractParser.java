/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.util;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * An AbstractParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public abstract class AbstractParser {
    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    /**
     * Reads and trims the element text and returns it or {@code null}
     *
     * @param reader  source for the element text
     * @return the string representing the trimmed element text or {@code null} if there is none or it is an empty string
     * @throws XMLStreamException
     */
    public String rawElementText(XMLStreamReader reader) throws XMLStreamException {
        String elementText = reader.getElementText();
        elementText = elementText == null || elementText.trim().length() == 0 ? null : elementText.trim();
        return elementText;
    }

    /**
     * Reads and trims the text for the given attribute and returns it or {@code null}
     *
     * @param reader source for the attribute text
     * @param attributeName  the name of the attribute
     * @return the string representing trimmed attribute text or {@code null} if there is none
     */
    public String rawAttributeText(XMLStreamReader reader, String attributeName) {
        return rawAttributeText(reader, attributeName, null);
    }

    /**
     * Reads and trims the text for the given attribute and returns it or {@code defaultValue} if there is no
     * value for the attribute
     * @param reader source for the attribute text
     * @param attributeName  the name of the attribute
     * @param defaultValue value to return if there is no value for the attribute
     * @return the string representing raw attribute text or {@code defaultValue} if there is none
     */
    public String rawAttributeText(XMLStreamReader reader, String attributeName, String defaultValue) {
        return reader.getAttributeValue("", attributeName) == null
                ? defaultValue :
                reader.getAttributeValue("", attributeName).trim();
    }


    protected void parseExtension(XMLExtendedStreamReader reader, String enclosingTag, final ModelNode operation,
                                  final SimpleAttributeDefinition extensionClassName, final PropertiesAttributeDefinition extensionProperties)
            throws XMLStreamException, ParserException, ValidateException {

        for (Extension.Attribute attribute : Extension.Attribute.values()) {
            switch (attribute) {
                case CLASS_NAME: {
                    requireSingleAttribute(reader, attribute.getLocalName());
                    final String value = reader.getAttributeValue(0);
                    extensionClassName.parseAndSetParameter(value, operation, reader);
                    break;

                }
                default:
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (reader.getLocalName().equals(enclosingTag)) {
                        //It's fine doing nothing
                        return;
                    } else {
                        if (Extension.Tag.forName(reader.getLocalName()) == Extension.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Extension.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            requireSingleAttribute(reader, "name");
                            final String name = reader.getAttributeValue(0);
                            String value = rawElementText(reader);
                            final String trimmed = value == null ? null : value.trim();
                            extensionProperties.parseAndAddParameterElement(name, trimmed, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

}
