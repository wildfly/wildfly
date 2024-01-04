/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import static org.mockito.Mockito.*;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * @author Paul Ferraro
 */
public class SimpleKeyFormatMapperTestCase {

    @Test
    public void test() {
        Formatter<Object> keyFormat = mock(Formatter.class);
        TwoWayKey2StringMapper mapper = new SimpleKeyFormatMapper(keyFormat);

        Object key = new Object();
        String formatted = "foo";

        when(keyFormat.getTargetClass()).thenReturn(Object.class);
        when(keyFormat.format(key)).thenReturn(formatted);
        when(keyFormat.parse(formatted)).thenReturn(key);

        Assert.assertSame(formatted, mapper.getStringMapping(key));
        Assert.assertSame(key, mapper.getKeyMapping(formatted));
        Assert.assertTrue(mapper.isSupportedType(Object.class));
        Assert.assertFalse(mapper.isSupportedType(Integer.class));
    }
}
