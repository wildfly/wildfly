/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.Function;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.KeyFormatter;
import org.wildfly.clustering.cache.KeySerializer;
import org.wildfly.clustering.ejb.client.SessionIDSerializer;
import org.wildfly.clustering.marshalling.Formatter;
import org.wildfly.clustering.marshalling.Serializer;

/**
 * {@link Serializer} for a {@link SessionID} based {@link Key}.
 * @author Paul Ferraro
 * @param <K> the key type
 */
public class SessionIDKeySerializer<K extends Key<SessionID>> extends KeySerializer<SessionID, K> {

    SessionIDKeySerializer(Function<SessionID, K> factory) {
        super(SessionIDSerializer.INSTANCE, factory);
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanMetaDataKeyFormatter extends KeyFormatter<SessionID, InfinispanBeanMetaDataKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanMetaDataKeyFormatter() {
            super((Class<InfinispanBeanMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanMetaDataKey.class, SessionIDSerializer.INSTANCE.toFormatter(SessionID.class), InfinispanBeanMetaDataKey::new);
        }
    }

    @MetaInfServices(Formatter.class)
    public static class InfinispanBeanGroupKeyFormatter extends KeyFormatter<SessionID, InfinispanBeanGroupKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanGroupKeyFormatter() {
            super((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, SessionIDSerializer.INSTANCE.toFormatter(SessionID.class), InfinispanBeanGroupKey::new);
        }
    }
}
