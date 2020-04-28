/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineImmutableSessionAttributes<K, V> implements ImmutableSessionAttributes {
    private final Map<String, UUID> names;
    private final Function<UUID, K> keyFactory;
    private final Map<K, V> attributeCache;
    private final Marshaller<Object, V> marshaller;

    public FineImmutableSessionAttributes(Map<String, UUID> names, Function<UUID, K> keyFactory, Map<K, V> attributeCache, Marshaller<Object, V> marshaller) {
        this.names = Collections.unmodifiableMap(names);
        this.keyFactory = keyFactory;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.keySet();
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;
        K key = this.keyFactory.apply(attributeId);
        return this.read(this.attributeCache.get(key));
    }

    private Object read(V value) {
        try {
            return this.marshaller.read(value);
        } catch (IOException e) {
            // This should not happen here, since attributes were pre-activated when session was constructed
            throw new IllegalStateException(e);
        }
    }
}
