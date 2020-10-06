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
import java.util.function.IntFunction;

import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.spi.ValueFunction;
import org.wildfly.clustering.marshalling.spi.util.HashSetExternalizer.CapacityFactory;

/**
 * @author Paul Ferraro
 */
public class HashMapMarshaller<T extends Map<Object, Object>> extends MapMarshaller<T, Void, Integer> {

    public HashMapMarshaller(Class<T> targetClass, IntFunction<T> factory) {
        super(targetClass, new CapacityFactory<>(factory), Map.Entry::getValue, ValueFunction.voidFunction(), PrimitiveMarshaller.VOID.cast(Void.class));
    }
}
