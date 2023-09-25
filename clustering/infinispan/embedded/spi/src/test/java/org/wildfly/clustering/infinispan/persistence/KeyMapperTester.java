/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import static org.junit.Assert.assertTrue;

import java.util.function.BiConsumer;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.junit.Assert;
import org.wildfly.clustering.marshalling.Tester;

/**
 * Tester for a {@link TwoWayKey2StringMapper}.
 * @author Paul Ferraro
 */
public class KeyMapperTester implements Tester<Object> {

    private final TwoWayKey2StringMapper mapper;

    public KeyMapperTester(TwoWayKey2StringMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void test(Object key) {
        this.test(key, Assert::assertEquals);
    }

    @Override
    public void test(Object key, BiConsumer<Object, Object> assertion) {
        assertTrue(this.mapper.isSupportedType(key.getClass()));

        String mapping = this.mapper.getStringMapping(key);

        Object result = this.mapper.getKeyMapping(mapping);

        assertion.accept(key, result);
    }
}
