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
package org.wildfly.clustering.web.infinispan.session.coarse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.NotSerializableException;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.web.infinispan.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;

/**
 * Exposes session attributes for a coarse granularity session.
 * @author Paul Ferraro
 */
public class CoarseSessionAttributes extends CoarseImmutableSessionAttributes implements SessionAttributes {
    private final Map<String, Object> attributes;
    private final Set<String> mutations;
    private final Mutator mutator;
    private final Marshallability marshallability;
    private final CacheProperties properties;

    public CoarseSessionAttributes(Map<String, Object> attributes, Mutator mutator, Marshallability marshallability, CacheProperties properties) {
        super(attributes);
        this.attributes = attributes;
        this.mutations = !properties.isTransactional() ? ConcurrentHashMap.newKeySet() : null;
        this.mutator = mutator;
        this.marshallability = marshallability;
        this.properties = properties;
    }

    @Override
    public Object removeAttribute(String name) {
        Object value = this.attributes.remove(name);
        this.mutator.mutate();
        if (this.mutations != null) {
            this.mutations.remove(name);
        }
        return value;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        if (this.properties.isMarshalling() && !this.marshallability.isMarshallable(value)) {
            throw new IllegalArgumentException(new NotSerializableException(value.getClass().getName()));
        }
        Object old = this.attributes.put(name, value);
        this.mutator.mutate();
        if (this.mutations != null) {
            this.mutations.remove(name);
        }
        return old;
    }

    @Override
    public Object getAttribute(String name) {
        Object value = this.attributes.get(name);
        if (!SessionAttributeImmutability.INSTANCE.test(value)) {
            if (this.mutations != null) {
                this.mutations.add(name);
            } else {
                this.mutator.mutate();
            }
        }
        return value;
    }

    @Override
    public void close() {
        if ((this.mutations != null) && !this.mutations.isEmpty()) {
            this.mutator.mutate();
        }
    }
}
