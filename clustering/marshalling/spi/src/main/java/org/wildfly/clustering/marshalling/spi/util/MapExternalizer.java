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
import java.util.AbstractMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizers for implementations of {@link Map}.
 * @author Paul Ferraro
 */
public class MapExternalizer<T extends Map<Object, Object>, C, CC> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<CC, T> factory;
    private final Function<Map.Entry<C, Integer>, CC> constructorContext;
    private final Function<T, C> context;
    private final Externalizer<C> contextExternalizer;

    protected MapExternalizer(Class<T> targetClass, Function<CC, T> factory, Function<Map.Entry<C, Integer>, CC> constructorContext, Function<T, C> context, Externalizer<C> contextExternalizer) {
        this.targetClass = targetClass;
        this.factory = factory;
        this.constructorContext = constructorContext;
        this.context = context;
        this.contextExternalizer = contextExternalizer;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        C context = this.context.apply(map);
        this.contextExternalizer.writeObject(output, context);
        IndexSerializer.VARIABLE.writeInt(output, map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        C context = this.contextExternalizer.readObject(input);
        int size = IndexSerializer.VARIABLE.readInt(input);
        CC constructorContext = this.constructorContext.apply(new AbstractMap.SimpleImmutableEntry<>(context, size));
        T map = this.factory.apply(constructorContext);
        for (int i = 0; i < size; ++i) {
            map.put(input.readObject(), input.readObject());
        }
        return map;
    }

    @Override
    public OptionalInt size(T map) {
        if (!map.isEmpty()) return OptionalInt.empty();
        C context = this.context.apply(map);
        OptionalInt contextSize = this.contextExternalizer.size(context);
        return contextSize.isPresent() ? OptionalInt.of(contextSize.getAsInt() + IndexSerializer.VARIABLE.size(map.size())) : OptionalInt.empty();
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
