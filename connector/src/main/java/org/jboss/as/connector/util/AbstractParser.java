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

import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.CommonLogger;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.logging.Logger;
import org.jboss.logging.Messages;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

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
     * convert an xml element in boolean value. Empty elements results with true (tag presence is sufficient condition)
     *
     * @param reader the StAX reader
     * @return the boolean representing element
     * @throws XMLStreamException StAX exception
     * @throws ParserException    in case of non valid boolean for given element value
     */
    protected Boolean elementAsBoolean(XMLStreamReader reader) throws XMLStreamException, ParserException {
        String elementtext = rawElementText(reader);
        String stringValue = getSubstitutionValue(elementtext);
        if (stringValue == null || stringValue.length() == 0 || stringValue.trim().equalsIgnoreCase("true") ||
                stringValue.trim().equalsIgnoreCase("false")) {

            return stringValue == null || stringValue.length() == 0 ? true : Boolean.valueOf(stringValue.trim());
        } else {
            throw new ParserException(bundle.elementAsBoolean(elementtext, reader.getLocalName()));
        }

    }

    /**
     * convert an xml attribute in boolean value. Empty elements results with false
     *
     * @param reader        the StAX reader
     * @param attributeName the name of the attribute
     * @return the boolean representing element
     * @throws XMLStreamException StAX exception
     * @throws ParserException    in case of not valid boolena for given attribute
     */
    protected Boolean attributeAsBoolean(XMLStreamReader reader, String attributeName)
            throws XMLStreamException, ParserException {
        String attributeString = rawAttributeText(reader, attributeName);
        String stringValue = getSubstitutionValue(attributeString);
        if (stringValue == null || stringValue.length() == 0 || stringValue.trim().equalsIgnoreCase("true") ||
                stringValue.trim().equalsIgnoreCase("false")) {

            return attributeString == null|| stringValue.trim().length() == 0
                    ? null :
                    Boolean.valueOf(reader.getAttributeValue("", attributeName).trim());
        } else {
            throw new ParserException(bundle.attributeAsBoolean(attributeString, reader.getLocalName()));
        }
    }

    /**
     * convert an xml element in String value
     *
     * @param reader the StAX reader
     * @return the string representing element
     * @throws XMLStreamException StAX exception
     */
    protected String elementAsString(XMLStreamReader reader) throws XMLStreamException {
        String elementtext = rawElementText(reader);
        //return elementtext;
        return getSubstitutionValue(elementtext);
    }

    /**
     * FIXME Comment this
     *
     * @param reader
     * @return the string representing the raw eleemnt text
     * @throws XMLStreamException
     */
    private String rawElementText(XMLStreamReader reader) throws XMLStreamException {
        String elementtext = reader.getElementText();
        elementtext = elementtext == null ? null : elementtext.trim();
        return elementtext;
    }

    /**
     * convert an xml element in String value
     *
     * @param reader        the StAX reader
     * @param attributeName the name of the attribute
     * @return the string representing element
     * @throws XMLStreamException StAX exception
     */
    protected String attributeAsString(XMLStreamReader reader, String attributeName) throws XMLStreamException {
        String attributeString = rawAttributeText(reader, attributeName);
        return getSubstitutionValue(attributeString);
    }

    /**
     * convert an xml element in String value
     *
     * @param reader        the StAX reader
     * @param attributeName the name of the attribute
     * @return the string representing element
     * @throws XMLStreamException StAX exception
     */
    protected Integer attributeAsInt(XMLStreamReader reader, String attributeName) throws XMLStreamException {
        String attributeString = getSubstitutionValue(rawAttributeText(reader, attributeName));
        return attributeString != null ? Integer.valueOf(getSubstitutionValue(attributeString)) : null;
    }

    /**
     * FIXME Comment this
     *
     * @param reader
     * @param attributeName
     * @return the string representing raw attribute textx
     */
    private String rawAttributeText(XMLStreamReader reader, String attributeName) {
        String attributeString = reader.getAttributeValue("", attributeName) == null ? null : reader.getAttributeValue(
                "", attributeName)
                .trim();
        return attributeString;
    }

    /**
     * convert an xml element in Integer value
     *
     * @param reader the StAX reader
     * @return the integer representing element
     * @throws XMLStreamException StAX exception
     * @throws ParserException    in case it isn't a number
     */
    protected Integer elementAsInteger(XMLStreamReader reader) throws XMLStreamException, ParserException {
        Integer integerValue;
        integerValue = null;
        String elementtext = rawElementText(reader);
        try {
            integerValue = Integer.valueOf(getSubstitutionValue(elementtext));
        } catch (NumberFormatException nfe) {
            throw new ParserException(bundle.notValidNumber(elementtext, reader.getLocalName()));
        }
        return integerValue;
    }

    /**
     * convert an xml element in Long value
     *
     * @param reader the StAX reader
     * @return the long representing element
     * @throws XMLStreamException StAX exception
     * @throws ParserException    in case it isn't a number
     */
    protected Long elementAsLong(XMLStreamReader reader) throws XMLStreamException, ParserException {
        Long longValue;
        longValue = null;
        String elementtext = rawElementText(reader);

        try {
            longValue = Long.valueOf(getSubstitutionValue(elementtext));
        } catch (NumberFormatException nfe) {
            throw new ParserException(bundle.notValidNumber(elementtext, reader.getLocalName()));
        }

        return longValue;
    }





    /**
     * System property substitution
     *
     * @param input The input string
     * @return The output
     */
    private String getSubstitutionValue(String input) throws XMLStreamException {
        if (input == null || input.trim().equals(""))
            return input;
        while ((input.indexOf("${")) != -1) {
            int from = input.indexOf("${");
            int to = input.indexOf("}");
            int dv = input.indexOf(":", from + 2);

            if (dv != -1) {
                if (dv > to)
                    dv = -1;
            }

            String systemProperty = "";
            String defaultValue = "";
            String s = input.substring(from + 2, to);
            if (dv == -1) {
                if ("/".equals(s)) {
                    systemProperty = File.separator;
                } else if (":".equals(s)) {
                    systemProperty = File.pathSeparator;
                } else {
                    systemProperty = SecurityActions.getSystemProperty(s);
                }
            } else {
                s = input.substring(from + 2, dv);
                systemProperty = SecurityActions.getSystemProperty(s);
                defaultValue = input.substring(dv + 1, to);
            }
            String prefix = "";
            String postfix = "";

            if (from != 0) {
                prefix = input.substring(0, from);
            }

            if (to + 1 < input.length() - 1) {
                postfix = input.substring(to + 1);
            }

            if (systemProperty != null && !systemProperty.trim().equals("")) {
                input = prefix + systemProperty + postfix;
            } else if (defaultValue != null && !defaultValue.trim().equals("")) {
                input = prefix + defaultValue + postfix;
            } else {
                input = prefix + postfix;
                log.debugf("System property %s not set", s);
            }
        }
        return input;
    }


    protected void parseExtension(XMLStreamReader reader, String enclosingTag, final ModelNode operation,
                                  final String extensionclassname, final String extensionProperties)
            throws XMLStreamException, ParserException, ValidateException {

        String className = null;
        Map<String, String> properties = null;

        for (Extension.Attribute attribute : Extension.Attribute.values()) {
            switch (attribute) {
                case CLASS_NAME: {
                    operation.get(extensionclassname).set(attributeAsString(reader, attribute.getLocalName()));
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
                            operation.get(extensionProperties, attributeAsString(reader, "name")).set(elementAsString(reader));
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
