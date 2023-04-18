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

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Function;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ejb.client.SessionIDSerializer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * {@link Serializer} for a {@link SessionID} based {@link Key}.
 * @author Paul Ferraro
 * @param <K> the key type
 */
public class SessionIDKeySerializer<K extends Key<SessionID>> implements Serializer<K> {

    private final Function<SessionID, K> factory;

    SessionIDKeySerializer(Function<SessionID, K> factory) {
        this.factory = factory;
    }

    @Override
    public void write(DataOutput output, K key) throws IOException {
        SessionIDSerializer.INSTANCE.write(output, key.getId());
    }

    @Override
    public K read(DataInput input) throws IOException {
        return this.factory.apply(SessionIDSerializer.INSTANCE.read(input));
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanCreationMetaDataKeyFormatter extends BinaryFormatter<InfinispanBeanCreationMetaDataKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanCreationMetaDataKeyFormatter() {
            super((Class<InfinispanBeanCreationMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanCreationMetaDataKey.class, new SessionIDKeySerializer<>(InfinispanBeanCreationMetaDataKey::new));
        }
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanAccessMetaDataKeyFormatter extends BinaryFormatter<InfinispanBeanAccessMetaDataKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanAccessMetaDataKeyFormatter() {
            super((Class<InfinispanBeanAccessMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanAccessMetaDataKey.class, new SessionIDKeySerializer<>(InfinispanBeanAccessMetaDataKey::new));
        }
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanGroupKeyFormatter extends BinaryFormatter<InfinispanBeanGroupKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanGroupKeyFormatter() {
            super((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, new SessionIDKeySerializer<>(InfinispanBeanGroupKey::new));
        }
    }
}
