/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.util.UUIDSerializer;

/**
 * Serializer for timer keys.
 * @author Paul Ferraro
 */
public class TimerKeySerializer<K extends Key<UUID>> implements Serializer<K> {

    private final Function<UUID, K> factory;

    TimerKeySerializer(Function<UUID, K> factory) {
        this.factory = factory;
    }

    @Override
    public void write(DataOutput output, K value) throws IOException {
        UUIDSerializer.INSTANCE.write(output, value.getId());
    }

    @Override
    public K read(DataInput input) throws IOException {
        return this.factory.apply(UUIDSerializer.INSTANCE.read(input));
    }

    @MetaInfServices(Formatter.class)
    public static class TimerCreationMetaDataKeyFormatter extends BinaryFormatter<TimerCreationMetaDataKey<UUID>> {
        @SuppressWarnings("unchecked")
        public TimerCreationMetaDataKeyFormatter() {
            super((Class<TimerCreationMetaDataKey<UUID>>) (Class<?>) TimerCreationMetaDataKey.class, new TimerKeySerializer<>(TimerCreationMetaDataKey::new));
        }
    }

    @MetaInfServices(Formatter.class)
    public static class TimerAccessMetaDataKeyFormatter extends BinaryFormatter<TimerAccessMetaDataKey<UUID>> {
        @SuppressWarnings("unchecked")
        public TimerAccessMetaDataKeyFormatter() {
            super((Class<TimerAccessMetaDataKey<UUID>>) (Class<?>) TimerAccessMetaDataKey.class, new TimerKeySerializer<>(TimerAccessMetaDataKey::new));
        }
    }
}
