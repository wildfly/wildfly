/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.SimpleFormatter;

/**
 * @author Paul Ferraro
 */
public class IndexedKeyFormatMapperTestCase {

    enum Type {
        TYPE00 {},
        TYPE01 {},
        TYPE02 {},
        TYPE03 {},
        TYPE04 {},
        TYPE05 {},
        TYPE06 {},
        TYPE07 {},
        TYPE08 {},
        TYPE09 {},
        TYPE10 {},
        TYPE11 {},
        TYPE12 {},
        TYPE13 {},
        TYPE14 {},
        TYPE15 {},
        TYPE16 {},
        TYPE17 {},
    }

    @Test
    public void testSinglePadding() {
        TwoWayKey2StringMapper mapper = new IndexedKeyFormatMapper(createPersistenceList(16));

        Assert.assertTrue(mapper.isSupportedType(Type.TYPE00.getClass()));
        Assert.assertTrue(mapper.isSupportedType(Type.TYPE15.getClass()));
        Assert.assertFalse(mapper.isSupportedType(Type.TYPE16.getClass()));
        Assert.assertFalse(mapper.isSupportedType(Type.TYPE17.getClass()));

        String result = mapper.getStringMapping(Type.TYPE00);
        Assert.assertSame(Type.TYPE00, mapper.getKeyMapping(result));
        Assert.assertEquals("0TYPE00", result);

        result = mapper.getStringMapping(Type.TYPE15);
        Assert.assertSame(Type.TYPE15, mapper.getKeyMapping(result));
        Assert.assertEquals("FTYPE15", result);
    }

    @Test
    public void testDoublePadding() {
        TwoWayKey2StringMapper mapper = new IndexedKeyFormatMapper(createPersistenceList(17));

        Assert.assertTrue(mapper.isSupportedType(Type.TYPE00.getClass()));
        Assert.assertTrue(mapper.isSupportedType(Type.TYPE15.getClass()));
        Assert.assertTrue(mapper.isSupportedType(Type.TYPE16.getClass()));
        Assert.assertFalse(mapper.isSupportedType(Type.TYPE17.getClass()));

        String result = mapper.getStringMapping(Type.TYPE00);
        Assert.assertSame(Type.TYPE00, mapper.getKeyMapping(result));
        Assert.assertEquals("00TYPE00", result);

        result = mapper.getStringMapping(Type.TYPE15);
        Assert.assertSame(Type.TYPE15, mapper.getKeyMapping(result));
        Assert.assertEquals("0FTYPE15", result);

        result = mapper.getStringMapping(Type.TYPE16);
        Assert.assertSame(Type.TYPE16, mapper.getKeyMapping(result));
        Assert.assertEquals("10TYPE16", result);
    }

    @SuppressWarnings("unchecked")
    private static List<? extends Formatter<?>> createPersistenceList(int size) {
        return java.util.stream.IntStream.range(0, size).mapToObj(index -> new SimpleFormatter<>((Class<Type>) Type.values()[index].getClass(), value -> Type.valueOf(value), Type::name)).collect(Collectors.toList());
    }
}
