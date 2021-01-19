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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;

/**
 * A class field that marshals instances of {@link Class} using a {@link ClassResolver}.
 * @author Paul Ferraro
 */
public class ClassResolverField implements Field<Class<?>> {

    private final ClassResolver resolver;
    private final int index;

    public ClassResolverField(ClassResolver resolver, int index) {
        this.resolver = resolver;
        this.index = index;
    }

    @Override
    public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        String className = AnyField.STRING.cast(String.class).readFrom(context, reader);
        return this.resolver.resolve(context, reader, className);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
        AnyField.STRING.writeTo(context, writer, targetClass.getName());
        this.resolver.annotate(context, writer, targetClass);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Class<?> targetClass) {
        OptionalInt size = this.resolver.size(context, targetClass);
        return size.isPresent() ? OptionalInt.of(size.getAsInt() + AnyField.STRING.size(context, targetClass.getName()).getAsInt()) : OptionalInt.empty();
    }

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return ClassField.ANY.getJavaClass();
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public int getWireType() {
        return WireFormat.WIRETYPE_LENGTH_DELIMITED;
    }
}
