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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for unmodifiable collections created via {@link java.util.List#of()} or {@link java.util.Set#of()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableCollectionMarshaller<E, T extends Collection<Object>> extends SimpleFunctionalMarshaller<T, Collection<Object>> {

    private static final ProtoStreamMarshaller<Collection<Object>> MARSHALLER = new CollectionMarshaller<>(LinkedList::new);

    public UnmodifiableCollectionMarshaller(Class<T> targetClass, Function<Object[], T> factory) {
        super(targetClass, MARSHALLER, new ExceptionFunction<>() {
            @Override
            public T apply(Collection<Object> collection) throws IOException {
                return factory.apply(collection.toArray());
            }
        });
    }
}
