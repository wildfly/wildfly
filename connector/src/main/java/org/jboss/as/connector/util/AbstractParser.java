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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.CommonLogger;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Logger;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A AbstractParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public abstract class AbstractParser {
    /**
     * The logger
     */
    private static CommonLogger log = Logger.getMessageLogger(CommonLogger.class, AbstractParser.class.getName());

    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);



    /**
     * FIXME Comment this
     *
     * @param reader
     * @return the string representing the raw eleemnt text
     * @throws XMLStreamException
     */
    public String rawElementText(XMLStreamReader reader) throws XMLStreamException {
        String elementtext = reader.getElementText();
        elementtext = elementtext == null || elementtext.trim().length() == 0 ? null : elementtext.trim();
        return elementtext;
    }

    /**
     * FIXME Comment this
     *
     * @param reader
     * @param attributeName
     * @return the string representing raw attribute textx
     */
    public String rawAttributeText(XMLStreamReader reader, String attributeName) {
        String attributeString = reader.getAttributeValue("", attributeName) == null ? null : reader.getAttributeValue(
                "", attributeName)
                .trim();
        return attributeString;
    }


    protected void parseExtension(XMLExtendedStreamReader reader, String enclosingTag, final ModelNode operation,
                                  final SimpleAttributeDefinition extensionclassname, final SimpleAttributeDefinition extensionProperties)
            throws XMLStreamException, ParserException, ValidateException {

        String className = null;
        Map<String, String> properties = null;

        for (Extension.Attribute attribute : Extension.Attribute.values()) {
            switch (attribute) {
                case CLASS_NAME: {
                    requireSingleAttribute(reader, attribute.getLocalName());
                    final String value = reader.getAttributeValue(0);
                    extensionclassname.parseAndSetParameter(value, operation, reader);
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
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
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
                            ModelNode node = new ModelNode();
                            if (trimmed != null ) {
                                if (extensionProperties.isAllowExpression()) {
                                    node = ParseUtils.parsePossibleExpression(trimmed);
                                } else {
                                    node = new ModelNode().set(trimmed);
                                }
                            }
                            operation.get(extensionProperties.getName(), name).set(node);
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

    private static class SecurityActions {
        /**
         * Constructor
         */
        private SecurityActions() {
        }

        /**
         * Get a system property
         *
         * @param name The property name
         * @return The property value
         */
        static String getSystemProperty(final String name) {
            if (System.getSecurityManager() == null) {
                return System.getProperty(name);
            } else {
                return (String) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return System.getProperty(name);
                    }
                });
            }
        }
    }


}
