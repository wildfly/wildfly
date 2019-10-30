/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Writes/reads an object to/from a binary stream.
 * @author Paul Ferraro
 */
public interface Serializer<T> {

    /**
     * Writes the specified object to the specified output stream
     * @param output the data output stream
     * @param value an object to serialize
     * @throws IOException if an I/O error occurs
     */
    void write(DataOutput output, T value) throws IOException;

    /**
     * Reads an object from the specified input stream.
     * @param input a data input stream
     * @return the deserialized object
     * @throws IOException if an I/O error occurs
     */
    T read(DataInput input) throws IOException;
}
