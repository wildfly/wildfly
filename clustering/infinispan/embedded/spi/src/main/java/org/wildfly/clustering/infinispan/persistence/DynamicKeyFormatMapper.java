/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.persistence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.SimpleFormatter;

/**
 * {@link org.infinispan.persistence.keymappers.TwoWayKey2StringMapper} implementation that based on a set of dynamically loaded {@link Formatter} instances.
 * @author Paul Ferraro
 */
public class DynamicKeyFormatMapper extends IndexedKeyFormatMapper {

    public DynamicKeyFormatMapper(ClassLoader loader) {
        super(load(loader));
    }

    private static List<Formatter<?>> load(ClassLoader loader) {
        List<Formatter<?>> keyFormats = new LinkedList<>();
        for (Formatter<?> keyFormat : ServiceLoader.load(Formatter.class, loader)) {
            keyFormats.add(keyFormat);
        }

        List<Formatter<?>> result = new ArrayList<>(keyFormats.size() + 6);
        // Add key formats for common key types
        result.add(new SimpleFormatter<>(String.class, Function.identity()));
        result.add(new SimpleFormatter<>(Byte.class, Byte::valueOf));
        result.add(new SimpleFormatter<>(Short.class, Short::valueOf));
        result.add(new SimpleFormatter<>(Integer.class, Integer::valueOf));
        result.add(new SimpleFormatter<>(Long.class, Long::valueOf));
        result.add(new SimpleFormatter<>(UUID.class, UUID::fromString));
        result.addAll(keyFormats);

        return result;
    }
}
