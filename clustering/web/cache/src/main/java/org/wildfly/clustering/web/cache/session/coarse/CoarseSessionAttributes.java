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
package org.wildfly.clustering.web.cache.session.coarse;

import java.io.NotSerializableException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.web.cache.session.SessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * Exposes session attributes for a coarse granularity session.
 * @author Paul Ferraro
 */
public class CoarseSessionAttributes extends CoarseImmutableSessionAttributes implements SessionAttributes {
    private final Map<String, Object> attributes;
    private final Mutator mutator;
    private final Marshallability marshallability;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final SessionActivationNotifier notifier;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public CoarseSessionAttributes(Map<String, Object> attributes, Mutator mutator, Marshallability marshallability, Immutability immutability, CacheProperties properties, SessionActivationNotifier notifier) {
        super(attributes);
        this.attributes = attributes;
        this.mutator = mutator;
        this.marshallability = marshallability;
        this.immutability = immutability;
        this.properties = properties;
        this.notifier = notifier;
        if (this.properties.isPersistent()) {
            this.notifier.postActivate();
        }
    }

    @Override
    public Object removeAttribute(String name) {
        Object value = this.attributes.remove(name);
        if (value != null) {
            this.dirty.set(true);
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
        // Always trigger mutation, even if this is an immutable object that was previously retrieved via getAttribute(...)
        this.dirty.set(true);
        return old;
    }

    @Override
    public Object getAttribute(String name) {
        Object value = this.attributes.get(name);
        if (!this.immutability.test(value)) {
            this.dirty.set(true);
        }
        return value;
    }

    @Override
    public void close() {
        if (this.properties.isPersistent()) {
            this.notifier.prePassivate();
        }

        if (this.dirty.compareAndSet(true, false)) {
            this.mutator.mutate();
        }
    }
}
