/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import org.jboss.staxmapper.XMLContentWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A generic model element.  Model elements are not generally thread-safe.
 *
 * @param <E> the concrete model element type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelElement<E extends AbstractModelElement<E>> implements Serializable, XMLContentWriter {

    private static final long serialVersionUID = 66064050420378211L;

    protected AbstractModelElement() {
        assert getClass() == getElementClass();
    }

    /**
     * Get an element hash consisting of the last 8 bytes of the given array.
     *
     * @param bytes the bytes
     * @return the element hash
     */
    protected static long elementHashOf(byte[] bytes) {
        assert bytes.length >= 8;
        long h = 0L;
        final int offs = bytes.length - 8;
        for (int i = 0; i < 8; i ++) {
            h = h << 8 | bytes[offs + i] & 0xffL;
        }
        return h;
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
     * @param <V> the map value (model element) type
     */
    protected final <K, V extends AbstractModelElement<V>> void calculateDifference(Collection<AbstractModelUpdate<E>> target, SortedMap<K, V> ourMap, SortedMap<K, V> theirMap, DifferenceHandler<K, V, E> handler) {
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
    protected interface DifferenceHandler<K, V extends AbstractModelElement<V>, E extends AbstractModelElement<E>> {

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
     * Get the concrete class of the element.
     *
     * @return the concrete class
     */
    protected abstract Class<E> getElementClass();

    /**
     * Determine if this model element is the same as (can replace) the other.
     *
     * @param other the other element
     *
     * @return {@code true} if they are the same element; {@code false} if they differ
     */
    public abstract boolean isSameElement(E other);

    /**
     * Write the content for this type.  The start element should have already been written.
     *
     * @param streamWriter the stream writer
     * @throws XMLStreamException if an error occurs
     */
    public abstract void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException;
}
