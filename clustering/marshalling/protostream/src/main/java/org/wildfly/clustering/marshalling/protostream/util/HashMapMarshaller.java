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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class HashMapMarshaller<T extends Map<Object, Object>> extends MapMarshaller<T, Void> {

    public HashMapMarshaller(Class<?> targetClass, Supplier<T> factory) {
        super(targetClass, new Function<Void, T>() {
            @Override
            public T apply(Void context) {
                return factory.get();
            }
        });
    }

    @Override
    protected Void readContext(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return null;
    }

    @Override
    protected void writeContext(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException {
    }

    @Override
    protected OptionalInt sizeContext(ImmutableSerializationContext context, T map) {
        return OptionalInt.of(0);
    }
}
