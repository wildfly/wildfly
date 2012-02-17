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
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.GroupCreationContext;

/**
 * @author Paul Ferraro
 *
 */
public class GroupAwareCache<K extends Serializable, V extends Cacheable<K>, G extends Serializable, M extends SerializationGroupMember<K, V, G>> extends SimpleCache<K, V, M> {

    private final GroupAwareBackingCache<K, V, G, M> groupedCache;
    private final boolean strictGroups;

    /**
     * @param groupedCache The backing cache entry store source from which this cache was created
     * @param strictGroups
     */
    public GroupAwareCache(final GroupAwareBackingCache<K, V, G, M> groupedCache, boolean strictGroups) {
        super(groupedCache, strictGroups);
        this.groupedCache = groupedCache;
        this.strictGroups = strictGroups;
    }

    @Override
    public V create() {
        boolean outer = false;
        GroupCreationContext<K, V, G, M, GroupAwareBackingCache<K, V, G, M>> groupContext = GroupCreationContext.getGroupCreationContext();
        if (groupContext == null) {
            groupContext = GroupCreationContext.startGroupCreationContext(this.strictGroups);
            outer = true;
        }
        List<Map.Entry<V, GroupAwareBackingCache<K, V, G, M>>> contextEntries = groupContext.getEntries();
        try {
            // Create our item. This may lead to nested calls to other caches
            V cacheItem = this.groupedCache.create().getUnderlyingItem();

            contextEntries.add(new AbstractMap.SimpleImmutableEntry<V, GroupAwareBackingCache<K, V, G, M>>(cacheItem, groupedCache));

            if (outer) {
                // If there is more than one item in the context, we need a group
                if (contextEntries.size() > 1) {
                    SerializationGroup<K, V, G> group = groupedCache.createGroup();
                    for (Map.Entry<V, GroupAwareBackingCache<K, V, G, M>> entry: contextEntries) {
                        V object = entry.getKey();
                        GroupAwareBackingCache<K, V, G, M> pairCache = entry.getValue();
                        pairCache.setGroup(object, group);
                    }
                }
            }
            return cacheItem;
        } catch (RuntimeException e) {
            if (outer) {
                // Clean up
                for (Map.Entry<V, GroupAwareBackingCache<K, V, G, M>> entry: contextEntries) {
                    V item = entry.getKey();
                    K id = item.getId();
                    try {
                        entry.getValue().remove(id);
                    } catch (Exception toLog) {
                        EjbLogger.EJB3_LOGGER.cacheRemoveFailed(id);
                    }
                }
            }
            throw e;
        } finally {
            if (outer) {
                GroupCreationContext.clearGroupCreationContext();
            }
        }
    }
}
