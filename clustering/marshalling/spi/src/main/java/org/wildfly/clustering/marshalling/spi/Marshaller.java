/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.marshalling.spi;

/**
 * A marshalling strategy for a specific object type.
 * @author Paul Ferraro
 * @param V the value type
 * @param S the serialized form type
 */
public interface Marshaller<V, S> extends Marshallability {

    /**
     * Reads a value from its marshalled form.
     * @param value the marshalled form
     * @return an unmarshalled value/
     * @throws InvalidSerializedFormException if the serialized form is invalid
     */
    V read(S value) throws InvalidSerializedFormException;

    /**
     * Writes a value to its serialized form
     * @param a value to marshal.
     * @return the serialized form of the value
     */
    S write(V value);
}
