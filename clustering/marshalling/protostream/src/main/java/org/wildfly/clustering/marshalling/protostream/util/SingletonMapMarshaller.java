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

import java.util.Map;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a singleton map.
 * @author Paul Ferraro
 */
public class SingletonMapMarshaller extends FunctionalMarshaller<Map<Object, Object>, SimpleEntry<Object, Object>> {
    private static final ExceptionFunction<Map<Object, Object>, SimpleEntry<Object, Object>, IOException> FUNCTION = new ExceptionFunction<Map<Object, Object>, SimpleEntry<Object, Object>, IOException>() {
        @Override
        public SimpleEntry<Object, Object> apply(Map<Object, Object> map) {
            return new SimpleEntry<>(map.entrySet().iterator().next());
        }
    };

    @SuppressWarnings("unchecked")
    public SingletonMapMarshaller(BiFunction<Object, Object, Map<Object, Object>> factory) {
        super((Class<Map<Object, Object>>) factory.apply(null, null).getClass(), new MapEntryMarshaller<>(Function.identity()), FUNCTION, new ExceptionFunction<SimpleEntry<Object, Object>, Map<Object, Object>, IOException>() {
            @Override
            public Map<Object, Object> apply(SimpleEntry<Object, Object> entry) {
                return factory.apply(entry.getKey(), entry.getValue());
            }
        });
    }
}
