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

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Abstract collection marshaller that writes the elements of the collection.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public abstract class AbstractCollectionMarshaller<T extends Collection<Object>> implements ProtoStreamMarshaller<T> {

    protected static final int ELEMENT_INDEX = 1;

    private final Class<? extends T> collectionClass;

    public AbstractCollectionMarshaller(Class<? extends T> collectionClass) {
        this.collectionClass = collectionClass;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T collection) throws IOException {
        for (Object element : collection) {
            writer.writeAny(ELEMENT_INDEX, element);
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.collectionClass;
    }
}
