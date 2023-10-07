/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.Function;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.KeySerializer;
import org.wildfly.clustering.ejb.client.SessionIDSerializer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * {@link Serializer} for a {@link SessionID} based {@link Key}.
 * @author Paul Ferraro
 * @param <K> the key type
 */
public class SessionIDKeySerializer<K extends Key<SessionID>> extends KeySerializer<K, SessionID> {

    SessionIDKeySerializer(Function<SessionID, K> factory) {
        super(SessionIDSerializer.INSTANCE, factory);
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanMetaDataKeyFormatter extends BinaryFormatter<InfinispanBeanMetaDataKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanMetaDataKeyFormatter() {
            super((Class<InfinispanBeanMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanMetaDataKey.class, new SessionIDKeySerializer<>(InfinispanBeanMetaDataKey::new));
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
