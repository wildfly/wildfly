/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Marshaller that uses a functional transformation of another marshaller.
 * @author Paul Ferraro
 */
public class FunctionalMarshaller<T, V> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final ProtoStreamMarshaller<V> marshaller;
    private final Function<T, V> function;
    private final Function<V, T> factory;

    public FunctionalMarshaller(Class<T> targetClass, ProtoStreamMarshaller<V> marshaller, Function<T, V> function, Function<V, T> factory) {
        this.targetClass = targetClass;
        this.marshaller = marshaller;
        this.function = function;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        V value = this.marshaller.readFrom(context, reader);
        return this.factory.apply(value);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T object) throws IOException {
        V value = this.function.apply(object);
        this.marshaller.writeTo(context, writer, value);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T object) {
        V value = this.function.apply(object);
        return this.marshaller.size(context, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
