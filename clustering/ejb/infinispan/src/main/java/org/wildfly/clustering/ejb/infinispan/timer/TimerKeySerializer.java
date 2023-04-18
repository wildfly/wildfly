/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
