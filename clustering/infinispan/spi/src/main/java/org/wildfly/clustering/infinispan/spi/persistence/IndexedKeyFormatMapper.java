/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;

/**
 * {@link TwoWayKey2StringMapper} implementation that maps multiple {@link KeyFormat} instances.
 * Key is mapped to an padded hexadecimal index + the formatted key.
 * @author Paul Ferraro
 */
public class IndexedKeyFormatMapper implements TwoWayKey2StringMapper {
    private static final int HEX_RADIX = 16;

    private final Map<Class<?>, Integer> indexes = new IdentityHashMap<>();
    private final List<KeyFormat<Object>> keyFormats;
    private final int padding;

    @SuppressWarnings("unchecked")
    public IndexedKeyFormatMapper(List<? extends KeyFormat<?>> keyFormats) {
        this.keyFormats = (List<KeyFormat<Object>>) (List<?>) keyFormats;
        for (int i = 0; i < this.keyFormats.size(); ++i) {
            this.indexes.put(this.keyFormats.get(i).getTargetClass(), i);
        }
        // Determine number of characters to reserve for index
        this.padding = (int) (Math.log(this.keyFormats.size() - 1) / Math.log(HEX_RADIX)) + 1;
    }

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return this.indexes.containsKey(keyType);
    }

    @Override
    public String getStringMapping(Object key) {
        Integer index = this.indexes.get(key.getClass());
        if (index == null) {
            throw new IllegalArgumentException(key.getClass().getName());
        }
        KeyFormat<Object> keyFormat = this.keyFormats.get(index);
        return String.format("%0" + this.padding + "X%s", index, keyFormat.format(key));
    }

    @Override
    public Object getKeyMapping(String value) {
        int index = Integer.parseUnsignedInt(value.substring(0, this.padding), HEX_RADIX);
        KeyFormat<Object> keyFormat = this.keyFormats.get(index);
        return keyFormat.parse(value.substring(this.padding));
    }
}
