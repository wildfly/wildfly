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

import javax.xml.XMLConstants;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;

/**
 * Enumerates the attributes used in the Infinispan subsystem schema.
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 RedHat Inc.
 */
public enum Attribute {
    // must be first
    UNKNOWN((String) null),
    ACQUIRE_TIMEOUT(ModelKeys.ACQUIRE_TIMEOUT),
    BATCH_SIZE(ModelKeys.BATCH_SIZE),
    BATCHING(ModelKeys.BATCHING),
    CACHE(ModelKeys.CACHE),
    CLASS(ModelKeys.CLASS),
    CONCURRENCY_LEVEL(ModelKeys.CONCURRENCY_LEVEL),
    DATASOURCE(ModelKeys.DATASOURCE),
    DEFAULT_CACHE(ModelKeys.DEFAULT_CACHE),
    DEFAULT_CACHE_CONTAINER(ModelKeys.DEFAULT_CACHE_CONTAINER),
    EAGER_LOCKING(ModelKeys.EAGER_LOCKING), /* deprecated */
    ENABLED(ModelKeys.ENABLED),
    EVICTION_EXECUTOR(ModelKeys.EVICTION_EXECUTOR),
    EXECUTOR(ModelKeys.EXECUTOR),
    FETCH_SIZE(ModelKeys.FETCH_SIZE),
    FETCH_STATE(ModelKeys.FETCH_STATE),
    FLUSH_TIMEOUT(ModelKeys.FLUSH_TIMEOUT),
    INDEXING(ModelKeys.INDEXING),
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
    REMOTE_TIMEOUT(ModelKeys.REMOTE_TIMEOUT),
    REPLICATION_QUEUE_EXECUTOR(ModelKeys.REPLICATION_QUEUE_EXECUTOR),
    SHARED(ModelKeys.SHARED),
    SINGLETON(ModelKeys.SINGLETON),
    SITE(ModelKeys.SITE),
    SOCKET_TIMEOUT(ModelKeys.SOCKET_TIMEOUT),
    STACK(ModelKeys.STACK),
    START(ModelKeys.START),
    STOP_TIMEOUT(ModelKeys.STOP_TIMEOUT),
    STRATEGY(ModelKeys.STRATEGY),
    STRIPING(ModelKeys.STRIPING),
    TCP_NO_DELAY(ModelKeys.TCP_NO_DELAY),
    TIMEOUT(ModelKeys.TIMEOUT),
    TYPE(ModelKeys.TYPE),
    VIRTUAL_NODES(ModelKeys.VIRTUAL_NODES),
    WAIT(ModelKeys.WAIT)
    ;

    private final String name;
    private final AttributeDefinition definition;

    private Attribute(final String name) {
        this.name = name;
        this.definition = null;
    }

    private Attribute(final AttributeDefinition definition) {
        this.name = definition.getXmlName();
        this.definition = definition;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    public AttributeDefinition getDefinition() {
        return definition;
    }

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
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
