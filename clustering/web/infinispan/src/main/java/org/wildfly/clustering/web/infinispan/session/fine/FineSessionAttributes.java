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

import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker.Operation;
import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.wildfly.clustering.web.infinispan.CacheEntryMutator;
import org.wildfly.clustering.web.infinispan.session.MutableDetector;
import org.wildfly.clustering.web.infinispan.session.SessionAttributeMarshaller;
import org.wildfly.clustering.web.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<V> extends FineImmutableSessionAttributes<V> implements SessionAttributes {
    private final Set<String> attributes;
    private final Cache<SessionAttributeCacheKey, V> cache;
    private final CacheInvoker invoker;
    private final SessionAttributeMarshaller<Object, V> marshaller;

    public FineSessionAttributes(String id, Set<String> attributes, Cache<SessionAttributeCacheKey, V> attributeCache, CacheInvoker invoker, SessionAttributeMarshaller<Object, V> marshaller) {
        super(id, attributes, attributeCache, invoker, marshaller);
        this.attributes = attributes;
        this.cache = attributeCache;
        this.invoker = invoker;
        this.marshaller = marshaller;
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributes.remove(name) ? this.marshaller.read(this.invoker.invoke(this.cache, new Remover.RemoveOperation<SessionAttributeCacheKey, V>(this.createKey(name)), Flag.FORCE_SYNCHRONOUS)) : null;
    }

    @Override
    public Object setAttribute(String name, Object attribute) {
        if (attribute == null) {
            return this.removeAttribute(name);
        }
        final SessionAttributeCacheKey key = this.createKey(name);
        final V value = this.marshaller.write(attribute);
        Operation<SessionAttributeCacheKey, V, V> operation = new Operation<SessionAttributeCacheKey, V, V>() {
            @Override
            public V invoke(Cache<SessionAttributeCacheKey, V> cache) {
                return cache.put(key, value);
            }
        };
        return this.marshaller.read(this.invoker.invoke(this.cache, operation, this.attributes.add(name) ? Flag.IGNORE_RETURN_VALUES : Flag.FORCE_SYNCHRONOUS));
    }

    @Override
    public Object getAttribute(String name) {
        SessionAttributeCacheKey key = this.createKey(name);
        V value = this.getAttributeValue(key);
        if (value == null) return null;
        Object attribute = this.marshaller.read(value);
        // If the object is mutable, we need to indicate that the attribute should be replicated
        if (MutableDetector.isMutable(attribute)) {
            new CacheEntryMutator<>(this.cache, this.invoker, key, value).mutate();
        }
        return attribute;
    }
}
