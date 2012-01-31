/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ParseUtils {

    private ParseUtils() {
    }

    public static Element nextElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.nextTag() == END_ELEMENT) {
            return null;
        }

        return Element.forName(reader.getLocalName());
    }

    /**
     * A variation of nextElement that verifies the nextElement is not in a different namespace.
     *
     * @param reader the XmlExtendedReader to read from.
     * @param expectedNamespace the namespace expected.
     * @return the element or null if the end is reached
     * @throws XMLStreamException if the namespace is wrong or there is a problem accessing the reader
     */
    public static Element nextElement(XMLExtendedStreamReader reader, Namespace expectedNamespace) throws XMLStreamException {
        Element element = nextElement(reader);

        if (element == null) {
            return element;
        } else if (expectedNamespace.equals(Namespace.forUri(reader.getNamespaceURI()))) {
            return element;
        }

        throw unexpectedElement(reader);
    }

    /**
     * Get an exception reporting an unexpected XML element.
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return MESSAGES.unexpectedElement(reader.getName(), reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected end tag for an XML element.
     * @param reader the stream reader
     * @return the exception
     */
    public static XMLStreamException unexpectedEndElement(final XMLExtendedStreamReader reader) {
        return MESSAGES.unexpectedEndElement(reader.getName(), reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return MESSAGES.unexpectedAttribute(reader.getAttributeName(index), reader.getLocation());
    }

    /**
     * Get an exception reporting an invalid XML attribute value.
     * @param reader the stream reader
     * @param index the attribute index
     * @return the exception
     */
    public static XMLStreamException invalidAttributeValue(final XMLExtendedStreamReader reader, final int index) {
        return MESSAGES.invalidAttributeValue(reader.getAttributeValue(index), reader.getAttributeName(index), reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return MESSAGES.missingRequiredAttributes(b, reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML child element.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingRequiredElement(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return MESSAGES.missingRequiredElements(b, reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML child element.
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the
     *        attribute name
     * @return the exception
     */
    public static XMLStreamException missingOneOf(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return MESSAGES.missingOneOf(b, reader.getLocation());
    }

    /**
     * Checks that the current element has no attributes, throwing an
     * {@link javax.xml.stream.XMLStreamException} if one is found.
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
    }

    /**
     * Consumes the remainder of the current element, throwing an
     * {@link javax.xml.stream.XMLStreamException} if it contains any child
     * elements.
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Require that the namespace of the current element matches the required namespace.
     *
     * @param reader the reader
     * @param requiredNs the namespace required
     * @throws XMLStreamException if the current namespace does not match the required namespace
     */
    public static void requireNamespace(final XMLExtendedStreamReader reader, final Namespace requiredNs) throws XMLStreamException {
        Namespace actualNs = Namespace.forUri(reader.getNamespaceURI());
        if (actualNs != requiredNs) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Get an exception reporting that an attribute of a given name has already
     * been declared in this scope.
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    public static XMLStreamException duplicateAttribute(final XMLExtendedStreamReader reader, final String name) {
        return MESSAGES.duplicateAttribute(name, reader.getLocation());
    }

    /**
     * Get an exception reporting that an element of a given type and name has
     * already been declared in this scope.
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    public static XMLStreamException duplicateNamedElement(final XMLExtendedStreamReader reader, final String name) {
        return MESSAGES.duplicateNamedElement(name, reader.getLocation());
    }

    /**
     * Read an element which contains only a single boolean attribute.
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @return the boolean value
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the
     *         element does not contain the specified attribute, contains other
     *         attributes, or contains child elements.
     */
    public static boolean readBooleanAttributeElement(final XMLExtendedStreamReader reader, final String attributeName)
            throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final boolean value = Boolean.parseBoolean(reader.getAttributeValue(0));
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single string attribute.
     * @param reader the reader
     * @param attributeName the attribute name, usually "value" or "name"
     * @return the string value
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the
     *         element does not contain the specified attribute, contains other
     *         attributes, or contains child elements.
     */
    public static String readStringAttributeElement(final XMLExtendedStreamReader reader, final String attributeName)
            throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final String value = reader.getAttributeValue(0);
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single list attribute of a given
     * type.
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the
     *         element does not contain the specified attribute, contains other
     *         attributes, or contains child elements.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> List<T> readListAttributeElement(final XMLExtendedStreamReader reader, final String attributeName,
            final Class<T> type) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        // todo: fix this when this method signature is corrected
        final List<T> value = (List<T>) reader.getListAttributeValue(0, type);
        requireNoContent(reader);
        return value;
    }

    public static Property readProperty(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int cnt = reader.getAttributeCount();
        String name = null;
        String value = null;
        for (int i = 0; i < cnt; i++) {
            String uri = reader.getAttributeNamespace(i);
            if (uri != null&&!"".equals(XMLConstants.NULL_NS_URI)) {
                throw unexpectedAttribute(reader, i);
            }
            final String localName = reader.getAttributeLocalName(i);
            if (localName.equals("name")) {
                name = reader.getAttributeValue(i);
            } else if (localName.equals("value")) {
                value = reader.getAttributeValue(i);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton("name"));
        }
        if (reader.next() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return new Property(name, new ModelNode().set(value == null ? "" : value));
    }

    /**
     * Read an element which contains only a single list attribute of a given
     * type, returning it as an array.
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list as an array
     * @throws javax.xml.stream.XMLStreamException if an error occurs or if the
     *         element does not contain the specified attribute, contains other
     *         attributes, or contains child elements.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T[] readArrayAttributeElement(final XMLExtendedStreamReader reader, final String attributeName,
            final Class<T> type) throws XMLStreamException {
        final List<T> list = readListAttributeElement(reader, attributeName, type);
        return list.toArray((T[]) Array.newInstance(type, list.size()));
    }

    /**
     * Require that the current element have only a single attribute with the
     * given name.
     * @param reader the reader
     * @param attributeName the attribute name
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static void requireSingleAttribute(final XMLExtendedStreamReader reader, final String attributeName)
            throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count == 0) {
            throw missingRequired(reader, Collections.singleton(attributeName));
        }
        requireNoNamespaceAttribute(reader, 0);
        if (!attributeName.equals(reader.getAttributeLocalName(0))) {
            throw unexpectedAttribute(reader, 0);
        }
        if (count > 1) {
            throw unexpectedAttribute(reader, 1);
        }
    }

    /**
     * Require all the named attributes, returning their values in order.
     * @param reader the reader
     * @param attributeNames the attribute names
     * @return the attribute values in order
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public static String[] requireAttributes(final XMLExtendedStreamReader reader, final String... attributeNames)
            throws XMLStreamException {
        final int length = attributeNames.length;
        final String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            final String name = attributeNames[i];
            final String value = reader.getAttributeValue(null, name);
            if (value == null) {
                throw missingRequired(reader, Collections.singleton(name));
            }
            result[i] = value;
        }
        return result;
    }

    public static boolean isNoNamespaceAttribute(final XMLExtendedStreamReader reader, final int index) {
        String namespace = reader.getAttributeNamespace(index);
        // FIXME when STXM-8 is done, remove the null check
        return namespace == null || XMLConstants.NULL_NS_URI.equals(namespace);
    }

    public static void requireNoNamespaceAttribute(final XMLExtendedStreamReader reader, final int index)
            throws XMLStreamException {
        if (!isNoNamespaceAttribute(reader, index)) {
            throw unexpectedAttribute(reader, index);
        }
    }

    public static ModelNode parseBoundedIntegerAttribute(final XMLExtendedStreamReader reader, final int index,
            final int minInclusive, final int maxInclusive, boolean allowExpression) throws XMLStreamException {
        final String stringValue = reader.getAttributeValue(index);
        if (allowExpression) {
            ModelNode expression = parsePossibleExpression(stringValue);
            if (expression.getType() == ModelType.EXPRESSION) {
                return expression;
            }
        }
        try {
            final int value = Integer.parseInt(stringValue);
            if (value < minInclusive || value > maxInclusive) {
                throw MESSAGES.invalidAttributeValue(value, reader.getAttributeName(index), minInclusive, maxInclusive, reader.getLocation());
            }
            return new ModelNode().set(value);
        } catch (NumberFormatException nfe) {
            throw MESSAGES.invalidAttributeValueInt(nfe, stringValue, reader.getAttributeName(index), reader.getLocation());
        }
    }



    public static ModelNode parsePossibleExpression(String value) {
        ModelNode result = new ModelNode();
        int openIdx = value.indexOf("${");
        if (openIdx > -1 && value.lastIndexOf('}') > openIdx) {
            result.setExpression(value);
        }
        else {
            result.set(value);
        }
        return result;
    }

    public static String getWarningMessage(final String msg, final Location location) {
        return MESSAGES.parsingProblem(location.getLineNumber(), location.getColumnNumber(), msg);
    }
}
