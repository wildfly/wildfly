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

package org.jboss.as.model;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * A generic model element.  Model elements are not generally thread-safe.
 *
 * @param <E> the concrete model element type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelElement<E extends AbstractModelElement<E>> implements Serializable, Cloneable, XMLContentWriter, XMLStreamConstants {

    private static final long serialVersionUID = 66064050420378211L;

    private final Location location;
    private final Set<AbstractModelElement<?>> children = new LinkedHashSet<AbstractModelElement<?>>(0);

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this model element
     */
    protected AbstractModelElement(final Location location) {
        assert getClass() == getElementClass();
        this.location = location;
    }

    /**
     * Construct a new instance initialized from the given XML stream.
     *
     * @param reader the stream reader
     */
    protected AbstractModelElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final javax.xml.stream.Location xmlLocation = reader.getLocation();
        location = new Location("<unknown-TODO>", xmlLocation.getLineNumber(), xmlLocation.getColumnNumber(), null);
    }

    /**
     * Get an element hash consisting of the last 8 bytes of the given array.
     *
     * @param bytes the bytes
     * @return the element hash
     */
    protected static long calculateElementHashOf(byte[] bytes) {
        assert bytes.length >= 8;
        long h = 0L;
        final int offs = bytes.length - 8;
        for (int i = 0; i < 8; i ++) {
            h = h << 8 | bytes[offs + i] & 0xffL;
        }
        return h;
    }

    /**
     * Calculate the cumulative element hash of an array of objects.  Changing the order of the objects will change
     * the result.
     *
     * @param objects the objects
     * @param initial the base hash (can be 0)
     * @return the modified hash
     */
    protected static long calculateElementHashOf(final Object[] objects, long initial) {
        for (Object o : objects) {
            if (o != null) initial = Long.rotateLeft(initial, 1) ^ o.hashCode() & 0xffffffffL;
        }
        return initial;
    }

    /**
     * Calculate the cumulative element hash of an array of enums.  Changing the order of the objects will change
     * the result.
     *
     * @param enums the enums
     * @param initial the base hash (can be 0)
     * @return the modified hash
     */
    protected static long calculateElementHashOf(final Enum<?>[] enums, long initial) {
        for (Enum<?> e : enums) {
            if (e != null) initial = Long.rotateLeft(initial, 1) ^ e.ordinal() & 0xffffffffL;
        }
        return initial;
    }

    /**
     * Calculate the cumulative element hash of an iterable sequence of elements.  In order to return consistent
     * results, the sequence should be sorted in some predictable order.
     *
     * @param elements the elements
     * @param initial the base hash (can be 0)
     * @return the modified hash
     */
    protected static long calculateElementHashOf(Iterable<? extends AbstractModelElement<?>> elements, long initial) {
        for (AbstractModelElement<?> element : elements) {
            initial = Long.rotateLeft(initial, 1) ^ element.elementHash();
        }
        return initial;
    }

    private static char[] table = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Convert a byte array into a hex string.
     *
     * @param bytes the bytes
     * @return the string
     */
    protected static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(table[b >> 4 & 0x0f]).append(table[b & 0x0f]);
        }
        return builder.toString();
    }

    /**
     * Get an exception reporting an unexpected XML element.
     *
     * @param reader the stream reader
     * @return the exception
     */
    protected static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting an unexpected XML attribute.
     *
     * @param reader the stream reader
     * @param index the element index
     * @return the exception
     */
    protected static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered", reader.getLocation());
    }

    /**
     * Get an exception reporting a missing, required XML attribute.
     *
     * @param reader the stream reader
     * @param required a set of enums whose toString method returns the attribute name
     * @return the exception
     */
    protected static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
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
     * Read an element which contains only a single boolean attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @return the boolean value
     * @throws XMLStreamException if an error occurs
     */
    protected static boolean readBooleanAttributeElement(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        try {
            return Boolean.parseBoolean(reader.getAttributeValue(0));
        } finally {
            consumeRemainder(reader);
        }
    }

    /**
     * Read an element which contains only a single string attribute.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value" or "name"
     * @return the string value
     * @throws XMLStreamException if an error occurs
     */
    protected static String readStringAttributeElement(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        try {
            return reader.getAttributeValue(0);
        } finally {
            consumeRemainder(reader);
        }
    }

    /**
     * Read an element which contains only a single list attribute of a given type.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list
     * @throws XMLStreamException if an error occurs
     */
    @SuppressWarnings({ "unchecked" })
    protected static <T> List<T> readListAttributeElement(final XMLExtendedStreamReader reader, final String attributeName, final Class<T> type) throws XMLStreamException {
        requireSingleAttribute(reader, attributeName);
        try {
            // todo: fix this when this method signature is corrected
            return (List<T>) reader.getListAttributeValue(0, type);
        } finally {
            consumeRemainder(reader);
        }
    }

    /**
     * Read an element which contains only a single list attribute of a given type, returning it as an array.
     *
     * @param reader the reader
     * @param attributeName the attribute name, usually "value"
     * @param type the value type class
     * @param <T> the value type
     * @return the value list as an array
     * @throws XMLStreamException if an error occurs
     */
    @SuppressWarnings({ "unchecked" })
    protected static <T> T[] readArrayAttributeElement(final XMLExtendedStreamReader reader, final String attributeName, final Class<T> type) throws XMLStreamException {
        final List<T> list = readListAttributeElement(reader, attributeName, type);
        return list.toArray((T[]) Array.newInstance(type, list.size()));
    }

    /**
     * Consume the remainder of this element.
     *
     * @param reader the reader
     * @throws XMLStreamException if an error occurs
     */
    protected static void consumeRemainder(final XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            final int t = reader.nextTag();
            switch (t) {
                case END_ELEMENT: return;
                case START_ELEMENT: throw unexpectedElement(reader);
                default: throw new IllegalStateException();
            }
        }
    }

    /**
     * Require that the current element have only a single attribute with the given name.
     *
     * @param reader the reader
     * @param attributeName the attribute name
     * @throws XMLStreamException if an error occurs
     */
    private static void requireSingleAttribute(final XMLExtendedStreamReader reader, final String attributeName) throws XMLStreamException {
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

    /**
     * Calculate a hash of this model element's complete contents.  This value is used to verify the state of the model
     * after applying a change; it should be unlikely (but is not guaranteed to be) to return the same value for two complete model
     * representations that differ by either small or large changes.
     *
     * @return the checksum
     */
    public abstract long elementHash();

    /**
     * Append the difference between this model element and the other model element to the target updates collection.
     *
     * @param target the collection
     * @param other the other element
     */
    protected abstract void appendDifference(Collection<AbstractModelUpdate<E>> target, E other);

    /**
     * Get the mutable set of child elements.
     *
     * @return the set
     */
    protected final Set<AbstractModelElement<?>> getChildren() {
        return children;
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
     * Helper method to implement the difference algorithm for sorted maps.  To use it, define an implementation of the
     * {@link DifferenceHandler} interface which will provide actions to take on add, remove, or change of a specific
     * element (as identified uniquely by the key in the map).
     *
     * @param target the target update collection
     * @param ourMap our view of the sorted map
     * @param theirMap the other model's view of the sorted map
     * @param handler the difference handler
     * @param <K> the map key type
     * @param <V> the map value type
     */
    protected final <K, V> void calculateDifference(Collection<AbstractModelUpdate<E>> target, SortedMap<K, V> ourMap, SortedMap<K, V> theirMap, DifferenceHandler<K, V, E> handler) {
        final Iterator<Map.Entry<K, V>> ourIterator = ourMap.entrySet().iterator();
        final Iterator<Map.Entry<K, V>> theirIterator = theirMap.entrySet().iterator();
        final Comparator<? super K> comparator = comparatorOf(ourMap);
        Map.Entry<K, V> ours = null;
        Map.Entry<K, V> theirs = null;
        K ourKey = null;
        K theirKey = null;
        int diff;
        if (ourIterator.hasNext() && theirIterator.hasNext()) {
            ours = ourIterator.next();
            theirs = theirIterator.next();
            for (;;) {
                ourKey = ours.getKey();
                theirKey = theirs.getKey();
                diff = comparator.compare(ourKey, theirKey);
                if (diff == 0) {
                    handler.handleChange(target, ourKey, ours.getValue(), theirs.getValue());
                    if (! (ourIterator.hasNext() && theirIterator.hasNext())) {
                        break;
                    }
                    ours = ourIterator.next();
                    theirs = theirIterator.next();
                } else if (diff < 0) {
                    // ours < theirs; our next item is missing from their set
                    handler.handleRemove(target, ourKey, ours.getValue());
                    if (! ourIterator.hasNext()) {
                        break;
                    }
                    ours = ourIterator.next();
                } else {
                    // ours > theirs; their next item is missing from our set
                    handler.handleAdd(target, theirKey, theirs.getValue());
                    if (! theirIterator.hasNext()) {
                        break;
                    }
                    theirs = theirIterator.next();
                }
            }
        }
        while (ourIterator.hasNext()) {
            ours = ourIterator.next();
            handler.handleRemove(target, ours.getKey(), ours.getValue());
        }
        while (theirIterator.hasNext()) {
            theirs = theirIterator.next();
            handler.handleAdd(target, theirs.getKey(), theirs.getValue());
        }
    }

    /**
     * A model difference handler.
     *
     * @param <K> the key type
     * @param <V> the model element type
     */
    protected interface DifferenceHandler<K, V, E extends AbstractModelElement<E>> {

        /**
         * Handle a model addition.
         *
         * @param target the collection to which updates should be appended
         * @param name the name (key) of the new model element
         * @param newElement the new model element
         */
        void handleAdd(Collection<AbstractModelUpdate<E>> target, K name, V newElement);

        /**
         * Handle a model removal.
         *
         * @param target the collection to which updates should be appended
         * @param name the name (key) of the removed model element
         * @param oldElement the removed model element
         */
        void handleRemove(Collection<AbstractModelUpdate<E>> target, K name, V oldElement);

        /**
         * Handle a model change.
         *
         * @param target the collection to which updates should be appended
         * @param name the name (key) of the model element
         * @param oldElement the old model element state
         * @param newElement the new model element state
         */
        void handleChange(Collection<AbstractModelUpdate<E>> target, K name, V oldElement, V newElement);
    }

    /**
     * Get this instance, cast to the concrete type.
     *
     * @return the concrete instance
     */
    public final E cast() {
        return getElementClass().cast(this);
    }

    /**
     * Cast another instance to this concrete type.
     *
     * @return the other instance
     */
    public final E cast(Object other) {
        return getElementClass().cast(other);
    }

    /**
     * Base clone method.
     *
     * @return the clone
     */
    protected E clone() {
        try {
            return cast(super.clone());
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the concrete class of the element.
     *
     * @return the concrete class
     */
    protected abstract Class<E> getElementClass();

    /**
     * Write the content for this type.  The start element will have already been written.
     *
     * @param streamWriter the stream writer
     * @throws XMLStreamException if an error occurs
     */
    public abstract void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException;

    /**
     * Get the declaration location of this element.
     *
     * @return the declaration location
     */
    public final Location getLocation() {
        return location;
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
