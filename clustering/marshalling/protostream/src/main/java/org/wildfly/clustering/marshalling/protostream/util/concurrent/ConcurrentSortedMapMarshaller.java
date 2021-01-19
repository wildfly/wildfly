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

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.SortedMapMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a concurrent {@link SortedMap} that does not allow null values.
 * @author Paul Ferraro
 */
public class ConcurrentSortedMapMarshaller<T extends ConcurrentMap<Object, Object> & SortedMap<Object, Object>> extends SimpleFunctionalMarshaller<T, SortedMap<Object, Object>> {
    private static final ProtoStreamMarshaller<SortedMap<Object, Object>> MARSHALLER = new SortedMapMarshaller<>(TreeMap::new);

    @SuppressWarnings("unchecked")
    public ConcurrentSortedMapMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) (Comparator<?>) Comparator.naturalOrder()).getClass(), MARSHALLER, new ExceptionFunction<SortedMap<Object, Object>, T, IOException>() {
            @Override
            public T apply(SortedMap<Object, Object> map) {
                T result = factory.apply(map.comparator());
                result.putAll(map);
                return result;
            }
        });
    }
}
