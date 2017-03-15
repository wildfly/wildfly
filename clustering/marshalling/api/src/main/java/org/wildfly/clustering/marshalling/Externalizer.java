/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Service provider interface for custom externalizers.
 * @author Paul Ferraro
 */
public interface Externalizer<T> {

    /**
     * Writes the object reference to the stream.
     *
     * @param output the object output to write to
     * @param object the object reference to write
     * @throws IOException if an I/O error occurs
     */
    void writeObject(ObjectOutput output, T object) throws IOException;

    /**
     * Read an instance from the stream.  The instance will have been written by the
     * {@link #writeObject(ObjectOutput, Object)} method.  Implementations are free
     * to create instances of the object read from the stream in any way that they
     * feel like. This could be via constructor, factory or reflection.
     *
     * @param input the object input from which to read
     * @return the object instance
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class could not be found
     */
    T readObject(ObjectInput input) throws IOException, ClassNotFoundException;

    /**
     * Returns the target class of the object to externalize.
     * @return a class to be externalized
     */
    Class<T> getTargetClass();
}
