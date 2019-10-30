/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.util.AbstractMap;

import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for {@link MapEntryExternalizer} externalizers
 * @author Paul Ferraro
 */
public class MapEntryExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        Object key = "key";
        Object value = "value";
        new ExternalizerTester<>(DefaultExternalizer.SIMPLE_ENTRY.cast(AbstractMap.SimpleEntry.class)).test(new AbstractMap.SimpleEntry<>(key, value));
        new ExternalizerTester<>(DefaultExternalizer.SIMPLE_IMMUTABLE_ENTRY.cast(AbstractMap.SimpleImmutableEntry.class)).test(new AbstractMap.SimpleImmutableEntry<>(key, value));
    }
}
