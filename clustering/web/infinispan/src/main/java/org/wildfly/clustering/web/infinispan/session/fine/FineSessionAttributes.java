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
package org.wildfly.clustering.web.infinispan.session.fine;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.web.infinispan.session.MutableDetector;
import org.wildfly.clustering.web.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<V> extends FineImmutableSessionAttributes<V> implements SessionAttributes {
    private final Cache<SessionAttributeCacheKey, V> cache;
    private final Marshaller<Object, V> marshaller;

    public FineSessionAttributes(String id, Cache<SessionAttributeCacheKey, V> attributeCache, Marshaller<Object, V> marshaller) {
        super(id, attributeCache, marshaller);
        this.cache = attributeCache;
        this.marshaller = marshaller;
    }

    @Override
    public Object removeAttribute(String name) {
        SessionAttributeCacheKey key = this.createKey(name);
        return this.read(name, this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(key));
    }

    @Override
    public Object setAttribute(String name, Object attribute) {
        if (attribute == null) {
            return this.removeAttribute(name);
        }
        SessionAttributeCacheKey key = this.createKey(name);
        V value = this.marshaller.write(attribute);
        return this.read(name, this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).put(key, value));
    }

    @Override
    public Object getAttribute(String name) {
        SessionAttributeCacheKey key = this.createKey(name);
        V value = this.cache.get(key);
        Object attribute = this.read(name, value);
        if (attribute != null) {
            // If the object is mutable, we need to indicate that the attribute should be replicated
            if (MutableDetector.isMutable(attribute)) {
                new CacheEntryMutator<>(this.cache, key, value).mutate();
            }
        }
        return attribute;
    }
}
