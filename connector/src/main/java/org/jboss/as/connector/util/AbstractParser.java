/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.util;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import java.lang.invoke.MethodHandles;
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
    private static CommonBundle bundle = Messages.getBundle(MethodHandles.lookup(), CommonBundle.class);


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


    protected void parseModuleExtension(XMLExtendedStreamReader reader, String enclosingTag, final ModelNode operation,
                                        final SimpleAttributeDefinition extensionClassName, final SimpleAttributeDefinition extensionModuleName,
                                        final PropertiesAttributeDefinition extensionProperties) throws XMLStreamException, ParserException, ValidateException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Extension.Attribute attribute = Extension.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS_NAME: {
                    final String value = reader.getAttributeValue(i);
                    extensionClassName.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MODULE: {
                    final String value = reader.getAttributeValue(i);
                    extensionModuleName.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
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
