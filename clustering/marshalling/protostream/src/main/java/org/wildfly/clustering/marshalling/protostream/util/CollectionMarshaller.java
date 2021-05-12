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
import java.util.function.Supplier;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;

/**
 * Marshaller for a basic collection.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public class CollectionMarshaller<T extends Collection<Object>> extends AbstractCollectionMarshaller<T> {

    private final Supplier<T> factory;

    @SuppressWarnings("unchecked")
   public CollectionMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass());
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T collection = this.factory.get();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case ELEMENT_INDEX:
                    collection.add(reader.readObject(Any.class).get());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return collection;
    }
}
