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

package org.jboss.as.metadata.parser.util;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author Emanuel Muckenhuber
 */
public class MetaDataElementParser implements XMLStreamConstants {

    public static class DTDInfo implements XMLResolver {

        private String publicID;
        private String systemID;
        private String baseURI;
        private String namespace;

        /** {@inheritDoc} */
        public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
            this.publicID = publicID;
            this.systemID = systemID;
            this.baseURI = baseURI;
            this.namespace = namespace;
            // FIXME
            return new ByteArrayInputStream(new byte[0]);
        }

        public String getPublicID() {
            return publicID;
        }

        public String getSystemID() {
            return systemID;
        }

        public String getBaseURI() {
            return baseURI;
        }

        public String getNamespace() {
            return namespace;
        }

        /** {@inheritDoc} */
        public String toString() {
            final StringBuilder builder = new StringBuilder(getClass().getSimpleName());
            builder.append("{");
            builder.append("publicID=").append(publicID).append(", ");
            builder.append("systemID=").append(systemID).append(", ");
            builder.append("baseURI=").append(baseURI).append(", ");
            builder.append("namespace=").append(namespace);
            builder.append("}");
            return builder.toString();
        }

    }

    /**
     * Get an exception reporting an unexpected XML element or attribute value.
     *
     * @param reader the stream reader
     * @return the exception
     */
    protected static XMLStreamException unexpectedValue(final XMLStreamReader reader, Throwable t) {
        return new XMLStreamException("Unexpected value '" + reader.getName() + "' encountered", reader.getLocation(), t);
    }

    /**
     * Get an exception reporting an unexpected XML element.
     *
     * @param reader the stream reader
     * @return the exception
     */
    protected static XMLStreamException unexpectedElement(final XMLStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     *
     * @param reader the stream reader
     * @param index the element index
     * @return the exception
     */
    protected static XMLStreamException unexpectedAttribute(final XMLStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     *
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the attribute name
     * @return the exception
     */
    protected static XMLStreamException missingRequired(final XMLStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required attribute(s): " + b, reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML child element.
     *
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the attribute name
     * @return the exception
     */
    protected static XMLStreamException missingRequiredElement(final XMLStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required element(s): " + b, reader.getLocation());
    }

    /**
     * Checks that the current element has no attributes, throwing an {@link XMLStreamException}
     * if one is found.
     *
     * @param reader the reader
     * @throws XMLStreamException if an error occurs
     */
    protected static void requireNoAttributes(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
    }

    /**
     * Consumes the remainder of the current element, throwing an {@link XMLStreamException}
     * if it contains any child elements.
     *
     * @param reader the reader
     * @throws XMLStreamException if an error occurs
     */
    protected static void requireNoContent(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Get an exception reporting that an element of a given type and name has already been declared in this scope.
     *
     * @param reader the stream reader
     * @param name the name that was redeclared
     * @return the exception
     */
    protected static XMLStreamException duplicateNamedElement(final XMLStreamReader reader, final String name) {
        return new XMLStreamException("An element of this type named '" + name + "' has already been declared", reader.getLocation());
    }

    /**
     * Read an element which contains only a single boolean attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @return the boolean value
     * @throws XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    protected static boolean readBooleanAttributeElement(final XMLStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final boolean value = Boolean.parseBoolean(reader.getAttributeValue(0));
        requireNoContent(reader);
        return value;
    }

    /**
     * Read an element which contains only a single string attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value" or "name"
     * @return the string value
     * @throws XMLStreamException if an error occurs or if the element does not
     *            contain the specified attribute, contains other attributes,
     *            or contains child elements.
     */
    protected static String readStringAttributeElement(final XMLStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        final String value = reader.getAttributeValue(0);
        requireNoContent(reader);
        return value;
    }

    /**
     * Require that the current element have only a single attribute with the given name.
     *
     * @param reader the reader
     * @param attributeName the attribute name
     * @throws XMLStreamException if an error occurs
     */
    private static void requireSingleAttribute(final XMLStreamReader reader, final String attributeName) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count == 0) {
            throw missingRequired(reader, Collections.singleton(attributeName));
        }
        if (reader.getAttributeNamespace(0) != null || ! attributeName.equals(reader.getAttributeLocalName(0))) {
            throw unexpectedAttribute(reader, 0);
        }
        if (count > 1) {
            throw unexpectedAttribute(reader, 1);
        }
    }

//    protected static Map<String, NamespaceAttribute> readNamespaces(final XMLStreamReader reader) {
//        int count = reader.getNamespaceCount();
//        Map<String, NamespaceAttribute> result = new HashMap<String, NamespaceAttribute>();
//        for (int i = 0; i < count; i++) {
//            String prefix = reader.getNamespacePrefix(i);
//            String uri = reader.getNamespaceURI(i);
//            result.put(uri, new NamespaceAttribute(prefix, uri));
//        }
//        return result;
//    }

    protected static String readSchemaLocation(final XMLStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count == 0) {
            return null;
        }
        String loc = null;
        for (int i = 0; i < count; i++) {
            if ("http://www.w3.org/2001/XMLSchema-instance".equals(reader.getAttributeNamespace(i))
                    && "schemaLocation".equals(reader.getAttributeLocalName(i))) {
                loc = reader.getAttributeValue(i);
                break;
            }
        }
        if (loc != null) {
            int pos = loc.indexOf(' ');
            if (pos > 0) {
                loc = loc.substring(pos + 1);
            }
        }
        return loc;
    }

    protected static String readDTDLocation(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != DTD) {
            return null;
        }
        String loc = reader.getText();
        int end = loc.lastIndexOf('"');
        if (end > 0) {
            int begin = loc.lastIndexOf('"', end - 1);
            if (begin > 0) {
                loc = loc.substring(begin + 1, end).trim();
            }
        }
        return loc;
    }

    protected static QName parseQName(String qname) {
        int begin = qname.indexOf('{');
        int end = qname.indexOf('}');
        if (begin < end && end < qname.length()) {
            String ns = qname.substring(begin + 1, end);
            String local = qname.substring(end + 1);
            return new QName(ns, local);
        } else {
            throw new IllegalArgumentException("Invalid QName: " + qname);
        }
    }

    /**
     * Returns a new {@link TreeMap} by passing the provided map to its constructor.
     * Thread safety note: <code>toCopy</code>'s monitor is held while the TreeMap
     * is being constructed.
     *
     * @param <K> the type of <code>toCopy</code>'s keys
     * @param <V> the type of <code>toCopy</code>'s values
     * @param toCopy the map to copy. Cannot be <code>null</code>
     * @return the copy
     */
    protected static <K, V> NavigableMap<K, V> safeCopyMap(NavigableMap<K, V> toCopy) {
        synchronized (toCopy) {
            return new TreeMap<K, V>(toCopy);
        }
    }


    private static final Comparator<Object> NATURAL = new Comparator<Object>() {
        @SuppressWarnings("unchecked")
        public int compare(final Object o1, final Object o2) {
            return ((Comparable) o1).compareTo(o2);
        }
    };

    private static <T> Comparator<? super T> comparatorOf(SortedMap<T, ?> map) {
        final Comparator<? super T> comparator = map.comparator();
        if (comparator != null) {
            return comparator;
        } else {
            return NATURAL;
        }
    }

    /**
     * Determine if this object is the same as the given object.  This is an identity comparison.
     *
     * @param obj the other object
     * @return {@code true} if the objects are the same
     */
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }

    /**
     * Get the identity hash code of this object.
     *
     * @return the identity hash code
     */
    public final int hashCode() {
        return super.hashCode();
    }

}
