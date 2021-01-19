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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.MapMarshaller;

/**
 * Marshaller for a {@link ConcurrentMap} that does not allow null values.
 * @author Paul Ferraro
 */
public class ConcurrentMapMarshaller<T extends ConcurrentMap<Object, Object>> extends SimpleFunctionalMarshaller<T, Map<Object, Object>> {
    private static final ProtoStreamMarshaller<Map<Object, Object>> MARSHALLER = new MapMarshaller<>(HashMap::new);

    @SuppressWarnings("unchecked")
    public ConcurrentMapMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass(), MARSHALLER, new Function<Map<Object, Object>, T>() {
            @Override
            public T apply(Map<Object, Object> map) {
                T result = factory.get();
                result.putAll(map);
                return result;
            }
        });
    }
}
