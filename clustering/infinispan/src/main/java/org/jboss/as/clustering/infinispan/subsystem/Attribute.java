/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;

/**
 * Enumerates the attributes used in the Infinispan subsystem schema.
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 RedHat Inc.
 * @author Tristan Tarrant
 */
public enum Attribute {
    // must be first
    UNKNOWN((String) null),
    ACQUIRE_TIMEOUT(ModelKeys.ACQUIRE_TIMEOUT),
    ALIASES(ModelKeys.ALIASES),
    ASYNC_MARSHALLING(ModelKeys.ASYNC_MARSHALLING),
    BACKUP_FAILURE_POLICY(ModelKeys.BACKUP_FAILURE_POLICY),
    BATCH_SIZE(ModelKeys.BATCH_SIZE),
    @Deprecated BATCHING(ModelKeys.BATCHING),
    CACHE(ModelKeys.CACHE),
    CAPACITY_FACTOR(ModelKeys.CAPACITY_FACTOR),
    CHUNK_SIZE(ModelKeys.CHUNK_SIZE),
    CLASS(ModelKeys.CLASS),
    CLUSTER(ModelKeys.CLUSTER),
    CONCURRENCY_LEVEL(ModelKeys.CONCURRENCY_LEVEL),
    CONSISTENT_HASH_STRATEGY(ModelKeys.CONSISTENT_HASH_STRATEGY),
    DATASOURCE(ModelKeys.DATASOURCE),
    DEFAULT_CACHE(ModelKeys.DEFAULT_CACHE),
    @Deprecated DEFAULT_CACHE_CONTAINER("default-cache-container"),
    DIALECT(ModelKeys.DIALECT),
    @Deprecated EAGER_LOCKING("eager-locking"),
    ENABLED(ModelKeys.ENABLED),
    EVICTION_EXECUTOR(ModelKeys.EVICTION_EXECUTOR),
    EXECUTOR(ModelKeys.EXECUTOR),
    FETCH_SIZE(ModelKeys.FETCH_SIZE),
    FETCH_STATE(ModelKeys.FETCH_STATE),
    FLUSH_LOCK_TIMEOUT(ModelKeys.FLUSH_LOCK_TIMEOUT),
    @Deprecated FLUSH_TIMEOUT("flush-timeout"),
    INDEXING(ModelKeys.INDEXING),
    INDEX(ModelKeys.INDEX),
    INTERVAL(ModelKeys.INTERVAL),
    ISOLATION(ModelKeys.ISOLATION),
    JNDI_NAME(ModelKeys.JNDI_NAME),
    L1_LIFESPAN(ModelKeys.L1_LIFESPAN),
    LIFESPAN(ModelKeys.LIFESPAN),
    LISTENER_EXECUTOR(ModelKeys.LISTENER_EXECUTOR),
    LOCK_TIMEOUT(ModelKeys.LOCK_TIMEOUT),
    LOCKING(ModelKeys.LOCKING),
    MACHINE(ModelKeys.MACHINE),
    MAX_ENTRIES(ModelKeys.MAX_ENTRIES),
    MAX_IDLE(ModelKeys.MAX_IDLE),
    MODE(ModelKeys.MODE),
    MODIFICATION_QUEUE_SIZE(ModelKeys.MODIFICATION_QUEUE_SIZE),
    MODULE(ModelKeys.MODULE),
    NAME(ModelKeys.NAME),
    NAMESPACE(XMLConstants.XMLNS_ATTRIBUTE),
    OUTBOUND_SOCKET_BINDING(ModelKeys.OUTBOUND_SOCKET_BINDING),
    OWNERS(ModelKeys.OWNERS),
    PASSIVATION(ModelKeys.PASSIVATION),
    PATH(ModelKeys.PATH),
    PREFIX(ModelKeys.PREFIX),
    PRELOAD(ModelKeys.PRELOAD),
    PURGE(ModelKeys.PURGE),
    QUEUE_FLUSH_INTERVAL(ModelKeys.QUEUE_FLUSH_INTERVAL),
    QUEUE_SIZE(ModelKeys.QUEUE_SIZE),
    RACK(ModelKeys.RACK),
    RELATIVE_TO(ModelKeys.RELATIVE_TO),
    REMOTE_CACHE(ModelKeys.REMOTE_CACHE),
    REMOTE_SITE(ModelKeys.REMOTE_SITE),
    REMOTE_TIMEOUT(ModelKeys.REMOTE_TIMEOUT),
    REPLICATION_QUEUE_EXECUTOR(ModelKeys.REPLICATION_QUEUE_EXECUTOR),
    SEGMENTS(ModelKeys.SEGMENTS),
    SHARED(ModelKeys.SHARED),
    SHUTDOWN_TIMEOUT(ModelKeys.SHUTDOWN_TIMEOUT),
    SINGLETON(ModelKeys.SINGLETON),
    SITE(ModelKeys.SITE),
    SOCKET_TIMEOUT(ModelKeys.SOCKET_TIMEOUT),
    STACK(ModelKeys.STACK),
    START(ModelKeys.START),
    STATISTICS_ENABLED(ModelKeys.STATISTICS_ENABLED),
    STOP_TIMEOUT(ModelKeys.STOP_TIMEOUT),
    STRATEGY(ModelKeys.STRATEGY),
    STRIPING(ModelKeys.STRIPING),
    TAKE_BACKUP_OFFLINE_AFTER_FAILURES(ModelKeys.TAKE_BACKUP_OFFLINE_AFTER_FAILURES),
    TAKE_BACKUP_OFFLINE_MIN_WAIT(ModelKeys.TAKE_BACKUP_OFFLINE_MIN_WAIT),
    TCP_NO_DELAY(ModelKeys.TCP_NO_DELAY),
    THREAD_POOL_SIZE(ModelKeys.THREAD_POOL_SIZE),
    TIMEOUT(ModelKeys.TIMEOUT),
    TYPE(ModelKeys.TYPE),
    @Deprecated VIRTUAL_NODES(ModelKeys.VIRTUAL_NODES),
    ;

    private final String name;

    private Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<>();
        for (Attribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) map.put(name, attribute);
        }
        attributes = map;
    }

    public static Attribute forName(String localName) {
        final Attribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
