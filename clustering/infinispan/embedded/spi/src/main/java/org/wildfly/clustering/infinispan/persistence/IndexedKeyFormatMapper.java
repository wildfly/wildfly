/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * {@link TwoWayKey2StringMapper} implementation that maps multiple {@link Formatter} instances.
 * Key is mapped to an padded hexadecimal index + the formatted key.
 * @author Paul Ferraro
 */
public class IndexedKeyFormatMapper implements TwoWayKey2StringMapper {
    private static final int HEX_RADIX = 16;

    private final Map<Class<?>, Integer> indexes = new IdentityHashMap<>();
    private final List<Formatter<Object>> keyFormats;
    private final int padding;

    @SuppressWarnings("unchecked")
    public IndexedKeyFormatMapper(List<? extends Formatter<?>> keyFormats) {
        this.keyFormats = (List<Formatter<Object>>) (List<?>) keyFormats;
        for (int i = 0; i < this.keyFormats.size(); ++i) {
            this.indexes.put(this.keyFormats.get(i).getTargetClass(), i);
        }
        // Determine number of characters to reserve for index
        this.padding = (int) (Math.log((double) this.keyFormats.size() - 1) / Math.log(HEX_RADIX)) + 1;
    }

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return this.indexes.containsKey(keyType);
    }

    @Override
    public String getStringMapping(Object key) {
        checkNotNullParam("key", key);
        Integer index = this.indexes.get(key.getClass());
        if (index == null) {
            throw new IllegalArgumentException(key.getClass().getName());
        }
        Formatter<Object> keyFormat = this.keyFormats.get(index);
        return String.format("%0" + this.padding + "X%s", index, keyFormat.format(key));
    }

    @Override
    public Object getKeyMapping(String value) {
        int index = Integer.parseUnsignedInt(value.substring(0, this.padding), HEX_RADIX);
        Formatter<Object> keyFormat = this.keyFormats.get(index);
        return keyFormat.parse(value.substring(this.padding));
    }
}
