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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Function;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;

/**
 * {@link TwoWayKey2StringMapper} implementation that based on a set of dynamically loaded {@link KeyFormat} instances.
 * @author Paul Ferraro
 */
public class DynamicKeyFormatMapper extends IndexedKeyFormatMapper {

    public DynamicKeyFormatMapper(ClassLoader loader) {
        super(load(loader));
    }

    private static List<KeyFormat<?>> load(ClassLoader loader) {
        List<KeyFormat<?>> keyFormats = new LinkedList<>();
        for (KeyFormat<?> keyFormat : ServiceLoader.load(KeyFormat.class, loader)) {
            keyFormats.add(keyFormat);
        }

        List<KeyFormat<?>> result = new ArrayList<>(keyFormats.size() + 6);
        // Add key formats for common key types
        result.add(new SimpleKeyFormat<>(String.class, Function.identity()));
        result.add(new SimpleKeyFormat<>(Byte.class, Byte::valueOf));
        result.add(new SimpleKeyFormat<>(Short.class, Short::valueOf));
        result.add(new SimpleKeyFormat<>(Integer.class, Integer::valueOf));
        result.add(new SimpleKeyFormat<>(Long.class, Long::valueOf));
        result.add(new SimpleKeyFormat<>(UUID.class, UUID::fromString));
        result.addAll(keyFormats);

        return result;
    }
}
