/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * Simple {@link TwoWayKey2StringMapper} based on a single {@link Formatter}.
 * @author Paul Ferraro
 */
public class SimpleKeyFormatMapper implements TwoWayKey2StringMapper {

    private final Formatter<Object> format;

    @SuppressWarnings("unchecked")
    public SimpleKeyFormatMapper(Formatter<?> format) {
        this.format = (Formatter<Object>) format;
    }

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return this.format.getTargetClass().equals(keyType);
    }

    @Override
    public String getStringMapping(Object key) {
        return this.format.format(key);
    }

    @Override
    public Object getKeyMapping(String value) {
        return this.format.parse(value);
    }
}
