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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for unmodifiable maps created via {@link java.util.Map#of()} or {@link java.util.Map#ofEntries()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableMapMarshaller<T extends Map<Object, Object>> extends SimpleFunctionalMarshaller<T, Map<Object, Object>> {
    private static final ProtoStreamMarshaller<Map<Object, Object>> MARSHALLER = new MapMarshaller<>(HashMap::new);

    public UnmodifiableMapMarshaller(Class<T> targetClass, Function<Map.Entry<? extends Object, ? extends Object>[], T> factory) {
        super(targetClass, MARSHALLER, new ExceptionFunction<Map<Object, Object>, T, IOException>() {
            @Override
            public T apply(Map<Object, Object> map) {
                @SuppressWarnings("unchecked")
                Map.Entry<Object, Object>[] entries = new Map.Entry[0];
                return factory.apply(map.entrySet().toArray(entries));
            }
        });
    }
}
