/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.cache.impl.backing;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntry;
import org.jboss.logging.Logger;

/**
 * Defines a group of serializable objects which must be serialized in one unit of work.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class SerializationGroupImpl<K extends Serializable, V extends Cacheable<K>> extends AbstractBackingCacheEntry<UUID, Cacheable<UUID>> implements SerializationGroup<K, V, UUID> {
    /** The serialVersionUID */
    private static final long serialVersionUID = -6181048392582344057L;

    private static final Logger log = Logger.getLogger(SerializationGroupImpl.class);

    private final UUID id;

    /**
     * The actual underlying objects passed in via addMember(). We store them here so they aren't lost when they are cleared
     * from the values stored in the "members" map.
     */
    private Map<K, V> memberObjects = new ConcurrentHashMap<K, V>();

    /**
     * The active group members. We don't serialize these. Rather, it is the responsibility of members to reassociate themselves
     * with the group upon deserialization (via addActive())
     */
    private transient Map<K, SerializationGroupMember<K, V, UUID>> active = new HashMap<K, SerializationGroupMember<K, V, UUID>>();

    /**
     * Set of keys passed to {@link #addInUse(Object)}
     */
    private transient Set<K> inUseKeys = new HashSet<K>();

    /** Transient ref to our group cache; used to validate compatibility */
    private transient PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache;

    /** Is this object used in a clustered cache? */
    private boolean clustered;

    private transient boolean groupModified;

    private transient ReentrantLock lock = new ReentrantLock();

    public SerializationGroupImpl(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Gets whether this groups supports (and requires) clustering functionality from its members.
     *
     * @return <code>true</code> if clustering is supported, <code>false</code> otherwise
     */
    public boolean isClustered() {
        return clustered;
    }

    /**
     * Sets whether this groups supports (and requires) clustering functionality from its members.
     *
     * @return
     */
    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    /**
     * Initially associates a new member with the group. Also {@link #addActive(SerializationGroupMember) marks the member as
     * active}.
     *
     * @param member
     *
     * @throws IllegalStateException if <code>member</code> was previously added to the group
     */
    @Override
    public void addMember(SerializationGroupMember<K, V, UUID> member) {
        K key = member.getId();
        if (this.memberObjects.containsKey(key)) {
            throw EjbMessages.MESSAGES.duplicateSerializationGroupMember(key, this.id);
        }
        log.tracef("Adding member %s to serialization group %s", key, this);
        this.memberObjects.put(key, member.getUnderlyingItem());
        active.put(key, member);
    }

    /**
     * Remove the specified member from the group.
     *
     * @param key the {@link Identifiable#getId() id} of the member
     */
    @Override
    public void removeMember(K key) {
        removeActive(key);
        this.memberObjects.remove(key);
    }

    /**
     * Gets the number of group members.
     */
    @Override
    public int size() {
        return this.memberObjects.size();
    }

    /**
     * Returns the {@link SerializationGroupMember#getUnderlyingItem() member object} associated with the member whose
     * {@link Identifiable#getId() id} matches <code>key</code>.
     *
     * @param key the {@link Identifiable#getId() id} of the member
     *
     * @return the object associated with the member, or <code>null</code> if <code>key</code> does not identify a member.
     */
    @Override
    public V getMemberObject(K key) {
        return this.memberObjects.get(key);
    }

    /**
     * Prepare members for passivation.
     */
    @Override
    public void prePassivate() {
        Iterator<SerializationGroupMember<K, V, UUID>> members = active.values().iterator();
        while (members.hasNext()) {
            SerializationGroupMember<K, V, UUID> member = members.next();
            member.prePassivate();
            members.remove();
        }
    }

    /**
     * Notification that the group has been activated from a passivated state.
     */
    @Override
    public void postActivate() {
    }

    /**
     * Prepare members for replication.
     */
    @Override
    public void preReplicate() {
        Iterator<SerializationGroupMember<K, V, UUID>> members = active.values().iterator();
        while (members.hasNext()) {
            SerializationGroupMember<K, V, UUID> member = members.next();
            member.preReplicate();
            members.remove();
        }
    }

    /**
     * Notification that the previously replicated group has been retrieved from a clustered cache.
     */
    @Override
    public void postReplicate() {
    }

    /**
     * Records that the given member is "active"; i.e. needs to have
     *
     * @PrePassivate callbacks invoked before serialization.
     *
     * @param member the member
     *
     * @throws IllegalStateException if <code>member</code> wasn't previously added to the group via
     *         {@link #addMember(SerializationGroupMember)}
     */
    @Override
    public void addActive(SerializationGroupMember<K, V, UUID> member) {
        K key = member.getId();
        active.put(key, member);
    }

    /**
     * Records that the given member is no longer "active"; i.e. does not need to have @PrePassivate callbacks invoked before
     * serialization.
     *
     * @param key the {@link Identifiable#getId() id} of the member
     *
     * @throws IllegalStateException if <code>member</code> wasn't previously added to the group via
     *         {@link #addMember(SerializationGroupMember)}
     */
    @Override
    public void removeActive(K key) {
        active.remove(key);
    }

    /**
     * Notification that the given member is "in use", and therefore the group should not be serialized.
     *
     * @param key the {@link Identifiable#getId() id} of the member
     *
     * @throws IllegalStateException if <code>member</code> wasn't previously added to the group via
     *         {@link #addMember(SerializationGroupMember)}
     */
    @Override
    public void addInUse(K key) {
        inUseKeys.add(key);
        setInUse(true);
    }

    /**
     * Notification that the given member is no longer "in use", and therefore should not prevent the group being serialized.
     *
     * @param key the {@link Identifiable#getId() id} of the member
     *
     * @throws IllegalStateException if <code>member</code> wasn't previously added to the group via
     *         {@link #addMember(SerializationGroupMember)}
     */
    @Override
    public void removeInUse(K key) {
        if (inUseKeys.remove(key)) {
            if (inUseKeys.size() == 0)
                setInUse(false);
            else
                setLastUsed(System.currentTimeMillis());
        } else if (!this.memberObjects.containsKey(key)) {
            throw EjbMessages.MESSAGES.missingSerializationGroupMember(key, this);
        }
    }

    /**
     * Gets the number of members currently in use.
     */
    @Override
    public int getInUseCount() {
        return inUseKeys.size();
    }

    @Override
    public boolean isModified() {
        boolean result = this.groupModified;
        setGroupModified(false);
        return result;
    }

    @Override
    public boolean isGroupModified() {
        return this.groupModified;
    }

    @Override
    public void setGroupModified(boolean modified) {
        this.groupModified = modified;
    }

    /**
     * FIXME -- returns null; what should it do?
     */
    @Override
    public Cacheable<UUID> getUnderlyingItem() {
        return null;
    }

    @Override
    public PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> getGroupCache() {
        return groupCache;
    }

    @Override
    public void lock() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw EjbMessages.MESSAGES.lockAcquisitionInterrupted(e, this);
        }
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public void setGroupCache(PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache) {
        this.groupCache = groupCache;
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        lock = new ReentrantLock();
        this.memberObjects = (Map<K, V>) in.readObject();
        active = new HashMap<K, SerializationGroupMember<K, V, UUID>>();
        inUseKeys = new HashSet<K>();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.memberObjects);
    }
}
