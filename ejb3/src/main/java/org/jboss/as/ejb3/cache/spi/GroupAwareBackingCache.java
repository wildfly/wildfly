/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.spi;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;

/**
 * A {@link BackingCache} that can manage the relationship of its underlying entries to any {@link SerializationGroup}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface GroupAwareBackingCache<K extends Serializable, V extends Cacheable<K>, G extends Serializable, M extends SerializationGroupMember<K, V, G>> extends PassivatingBackingCache<K, V, M> {
    /**
     * Create a {@link SerializationGroup} to contain objects cached by this object.
     *
     * @return a {@link SerializationGroup}
     */
    SerializationGroup<K, V, G> createGroup();

    /**
     * Assign the given object to the given group. The group will be of the {@link SerializationGroup} implementation type
     * returned by {@link #createGroup()}.
     *
     * @param obj
     * @param group
     *
     * @throws IllegalStateException if the group's cache is incompatible with ourself.
     */
    void setGroup(V obj, SerializationGroup<K, V, G> group);

    /**
     * Callback from the group informing the cache it needs to invoke pre-replication callbacks on the member.
     *
     * @param member the group member
     *
     * @throws IllegalStateException if the member is {@link BackingCacheEntry#isInUse() in-use}.
     */
    void notifyPreReplicate(M member);
}
