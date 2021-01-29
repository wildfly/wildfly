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
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.wildfly.common.function.Functions;

/**
 * ProtoStream marshaller for fixed values.
 * @author Paul Ferraro
 */
public class ValueMarshaller<T> implements ProtoStreamMarshaller<T> {

    private static final OptionalInt SIZE = OptionalInt.of(0);

    private final Class<T> targetClass;
    private final Supplier<T> factory;

    public ValueMarshaller(T value) {
        this(Functions.constantSupplier(value));
    }

    @SuppressWarnings("unchecked")
    public ValueMarshaller(Supplier<T> factory) {
        this.targetClass = (Class<T>) factory.get().getClass();
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        return this.factory.get();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) {
        // Nothing to write
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        return SIZE;
    }
}
