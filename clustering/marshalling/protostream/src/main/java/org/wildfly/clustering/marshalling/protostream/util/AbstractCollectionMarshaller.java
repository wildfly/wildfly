/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import java.util.Collection;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Abstract collection marshaller that writes the elements of the collection.
 * @author Paul Ferraro
 */
public abstract class AbstractCollectionMarshaller<T extends Collection<Object>> implements ProtoStreamMarshaller<T> {

    protected static final int ELEMENT_INDEX = 1;

    private final Class<? extends T> collectionClass;

    public AbstractCollectionMarshaller(Class<? extends T> collectionClass) {
        this.collectionClass = collectionClass;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T collection) throws IOException {
        for (Object element : collection) {
            writer.writeBytes(ELEMENT_INDEX, ProtoStreamMarshaller.write(context, new Any(element)));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T collection) {
        int size = 0;
        for (Object element : collection) {
            OptionalInt elementSize = Predictable.computeSize(context, ELEMENT_INDEX, new Any(element));
            if (elementSize.isPresent()) {
                size += elementSize.getAsInt();
            } else {
                return elementSize;
            }
        }
        return OptionalInt.of(size);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.collectionClass;
    }
}
