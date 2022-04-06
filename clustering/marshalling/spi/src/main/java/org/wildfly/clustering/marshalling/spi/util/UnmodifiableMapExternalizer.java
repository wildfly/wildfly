/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
 * Externalizer for unmodifiable maps created via {@link java.util.Map#of()} or {@link java.util.Map#ofEntries()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableMapExternalizer<T extends Map<Object, Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Map.Entry<Object, Object>[], T> factory;

    public UnmodifiableMapExternalizer(Class<T> targetClass, Function<Map.Entry<Object, Object>[], T> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object>[] entries = new Map.Entry[IndexSerializer.VARIABLE.readInt(input)];
        for (int i = 0; i < entries.length; ++i) {
            entries[i] = Map.entry(input.readObject(), input.readObject());
        }
        return this.factory.apply(entries);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
