/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;

/**
 * Function that puts an entry into a map.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class MapPutFunction<K, V> extends MapFunction<K, V, Map.Entry<K, V>> {

    public MapPutFunction(Map.Entry<K, V> operand, Operations<Map<K, V>> operations) {
        super(operand, operations, operations);
    }

    @Override
    public Map.Entry<K, V> getOperand() {
        return super.getOperand();
    }

    @Override
    public void accept(Map<K, V> map, Map.Entry<K, V> entry) {
        map.put(entry.getKey(), entry.getValue());
    }
}
