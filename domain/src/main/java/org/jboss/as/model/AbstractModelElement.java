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

import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.io.Serializable;

/**
 * A generic model element.  Model elements are not generally thread-safe.
 *
 * @param <E> the concrete model element type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelElement<E extends AbstractModelElement<E>> implements Serializable, Cloneable, XMLContentWriter, XMLStreamConstants {

    private static final long serialVersionUID = 66064050420378211L;

    // FIXME make non-transient and final when MSC-16 is fixed

    /**
     * Construct a new instance.
     */
    protected AbstractModelElement() {
        assert getClass() == getElementClass() : "" + getClass() + " != " + getElementClass();
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
     * Convert a hex string into a byte[].
     *
     * @param s the string
     * @return the bytes
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int x = Character.digit(s.charAt(j), 16) << 4;
            j++;
            x = x | Character.digit(s.charAt(j), 16);
            j++;
            data[i] = (byte) (x & 0xFF);
        }
        return data;
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
     * Write the content for this type.  The start element will have already been written.
     *
     * @param streamWriter the stream writer
     * @throws XMLStreamException if an error occurs
     */
    public abstract void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException;

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
