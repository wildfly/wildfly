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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizers for implementations of {@link Map}.
 * @author Paul Ferraro
 */
public abstract class MapExternalizer<T extends Map<Object, Object>, C> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<C, T> factory;

    @SuppressWarnings("unchecked")
    protected MapExternalizer(Class<?> targetClass, Function<C, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        this.writeContext(output, map);
        IndexSerializer.VARIABLE.writeInt(output, map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        C context = this.readContext(input);
        T map = this.factory.apply(context);
        int size = IndexSerializer.VARIABLE.readInt(input);
        for (int i = 0; i < size; ++i) {
            map.put(input.readObject(), input.readObject());
        }
        return map;
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    /**
     * Writes the context of the specified map to the specified output stream.
     * @param output an output stream
     * @param map the target map
     * @throws IOException if the constructor context cannot be written to the stream
     */
    protected abstract void writeContext(ObjectOutput output, T map) throws IOException;

    /**
     * Reads the map context from the specified input stream.
     * @param input an input stream
     * @return the map constructor context
     * @throws IOException if the constructor context cannot be read from the stream
     * @throws ClassNotFoundException if a class could not be found
     */
    protected abstract C readContext(ObjectInput input) throws IOException, ClassNotFoundException;
}
