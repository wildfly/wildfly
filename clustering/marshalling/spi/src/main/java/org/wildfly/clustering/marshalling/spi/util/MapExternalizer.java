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
import java.util.function.IntFunction;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizers for implementations of {@link Map}.
 * @author Paul Ferraro
 */
public class MapExternalizer<T extends Map<Object, Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final IntFunction<T> factory;

    @SuppressWarnings("unchecked")
    public MapExternalizer(Class<?> targetClass, IntFunction<T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        writeMap(output, map);
    }

    static <T extends Map<Object, Object>> void writeMap(ObjectOutput output, T map) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = IndexSerializer.VARIABLE.readInt(input);
        return readMap(input, this.factory.apply(size), size);
    }

    static <T extends Map<Object, Object>> T readMap(ObjectInput input, T map, int size) throws IOException, ClassNotFoundException {
        for (int i = 0; i < size; ++i) {
            map.put(input.readObject(), input.readObject());
        }
        return map;
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
