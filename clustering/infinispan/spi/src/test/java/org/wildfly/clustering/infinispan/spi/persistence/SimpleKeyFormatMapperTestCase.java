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

import static org.mockito.Mockito.*;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleKeyFormatMapperTestCase {

    @Test
    public void test() {
        KeyFormat<Object> keyFormat = mock(KeyFormat.class);
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
