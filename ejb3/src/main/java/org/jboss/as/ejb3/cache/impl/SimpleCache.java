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

package org.jboss.as.ejb3.cache.impl;

import java.io.Serializable;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCache;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractCache;
import org.jboss.as.ejb3.cache.spi.impl.GroupCreationContext;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;

/**
 * @author Paul Ferraro
 *
 */
public class SimpleCache<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> extends AbstractCache<K, V, E> {
    private final boolean strictGroups;

    public SimpleCache(BackingCache<K, V, E> backingCache, boolean strictGroups) {
        super(backingCache);
        this.strictGroups = strictGroups;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#create()
     */
    @Override
    public V create() {
        boolean outer = false;
        GroupCreationContext<K, V, Serializable, SerializationGroupMember<K, V, Serializable>, GroupAwareBackingCache<K, V, Serializable, SerializationGroupMember<K, V, Serializable>>> groupContext = GroupCreationContext.getGroupCreationContext();
        if (groupContext != null) {
            // There's a nested hierarchy being formed, but we can't participate
            // in a serialization group. If we're configured to object or the
            // group itself is configured to object, we throw an ISE
            if (groupContext.isStrict() || (this.strictGroups && groupContext.getEntries().size() > 0)) {
                throw EjbMessages.MESSAGES.incompatibleCaches();
            }
        } else {
            GroupCreationContext.<K, V, Serializable, SerializationGroupMember<K, V, Serializable>, GroupAwareBackingCache<K, V, Serializable, SerializationGroupMember<K, V, Serializable>>>startGroupCreationContext(this.strictGroups);
            outer = true;
        }

        try {
            return super.create();
        } finally {
            if (outer) {
                GroupCreationContext.clearGroupCreationContext();
            }
        }
    }

    @Override
    public Affinity getStrictAffinity() {
        final String nodeName = SecurityActions.getSystemProperty(ServerEnvironment.NODE_NAME);
        return new NodeAffinity(nodeName);
    }

    @Override
    public Affinity getWeakAffinity() {
        return Affinity.NONE;
    }
}
