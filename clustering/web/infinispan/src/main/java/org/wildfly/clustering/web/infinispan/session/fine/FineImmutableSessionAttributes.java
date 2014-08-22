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
import org.wildfly.clustering.web.infinispan.session.SessionAttributeMarshaller;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineImmutableSessionAttributes<V> implements ImmutableSessionAttributes {
    private final String id;
    private final Set<String> attributes;
    private final Cache<SessionAttributeCacheKey, V> cache;
    private final SessionAttributeMarshaller<Object, V> marshaller;

    public FineImmutableSessionAttributes(String id, Set<String> attributes, Cache<SessionAttributeCacheKey, V> attributeCache, SessionAttributeMarshaller<Object, V> marshaller) {
        this.id = id;
        this.attributes = attributes;
        this.cache = attributeCache;
        this.marshaller = marshaller;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.attributes;
    }

    @Override
    public Object getAttribute(String name) {
        V value = this.getAttributeValue(this.createKey(name));
        return (value != null) ? this.marshaller.read(value) : null;
    }

    protected V getAttributeValue(SessionAttributeCacheKey key) {
        return this.cache.get(key);
    }

    protected SessionAttributeCacheKey createKey(String attribute) {
        return new SessionAttributeCacheKey(this.id, attribute);
    }
}
