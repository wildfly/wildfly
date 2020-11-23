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

package org.wildfly.clustering.ee;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Strategy for managing the creation and destruction of objects.
 * @author Paul Ferraro
 */
public interface Manager<K, V> extends BiFunction<K, Function<Runnable, V>, V> {
    /**
     * Returns the value associated with the specified key, creating it from the specified factory, if necessary.
     * @param key the key of the managed value
     * @param factory a factory for creating the value from a close task
     * @return a managed value
     */
    @Override
    V apply(K key, Function<Runnable, V> factory);
}
