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

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 * Externalizer for hash table based sets constructed with a capacity rather than a size.
 * @author Paul Ferraro
 */
public class HashSetExternalizer<T extends Set<Object>> extends BoundedCollectionExternalizer<T> {
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final IntUnaryOperator CAPACITY = new IntUnaryOperator() {
        @Override
        public int applyAsInt(int size) {
            // Generate a suitable capacity for a given initial size
            return size * 2;
        }
    };

    public HashSetExternalizer(Class<T> targetClass, IntFunction<T> factory) {
        super(targetClass, new CapacityFactory<>(factory));
    }

    /**
     * Creates a hash table based map or collection with an appropriate capacity given an initial size.
     * @param <T> the map or collection type.
     */
    public static class CapacityFactory<T> implements Function<Integer, T>, IntFunction<T> {
        private final IntFunction<T> factory;

        public CapacityFactory(IntFunction<T> factory) {
            this.factory = factory;
        }

        @Override
        public T apply(Integer size) {
            return this.apply(size.intValue());
        }

        @Override
        public T apply(int size) {
            return this.factory.apply(CAPACITY.applyAsInt(size));
        }
    }
}
