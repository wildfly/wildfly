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

package org.jboss.as.ejb3.cache.impl.backing;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntry;
import org.jboss.logging.Logger;

/**
 * A member of a {@link SerializationGroupImpl}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class SerializationGroupMemberImpl<K extends Serializable, V extends Cacheable<K>, G extends Serializable> extends AbstractBackingCacheEntry<K, V> implements SerializationGroupMember<K, V, G> {
    /** The serialVersionUID */
    private static final long serialVersionUID = 7268142730501106252L;

    private static final Logger log = Logger.getLogger(SerializationGroupMemberImpl.class);

    /**
     * Identifier for our underlying object
     */
    private K id;

    /**
     * The underlying object (e.g. bean context).
     */
    private transient V value;

    /** The group. Never serialize the group; only the groupCache does that */
    private transient SerializationGroup<K, V, G> group;

    /**
     * Id for our group; serialize this so we can find our group again after deserialization on a remote node.
     */
    private G groupId;

    private boolean clustered;

    private boolean preReplicated;

    private transient ReentrantLock lock = new ReentrantLock();
    private transient boolean groupLockHeld;

    /** The cache that's handling us */
    private transient GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> cache;

    public SerializationGroupMemberImpl(V obj, GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> cache) {
        this.value = obj;
        this.id = obj.getId();
        this.cache = cache;
        this.clustered = cache.isClustered();
    }

    @Override
    public K getId() {
        return id;
    }

    @Override
    public boolean isModified() {
        boolean localModified = (this.value != null && this.value.isModified());
        if (localModified && group != null)
            group.setGroupModified(true);
        return (group == null ? localModified : group.isGroupModified());
    }

    /**
     * Gets whether this member supports clustering functionality.
     *
     * @return <code>true</code> if clustering is supported, <code>false</code> otherwise
     */
    public boolean isClustered() {
        return clustered;
    }

    @Override
    public V getUnderlyingItem() {
        return this.value;
    }

    /**
     * Sets the underlying {@link Cacheable} associated with this group member.
     *
     * @param item the cache item
     */
    @Override
    public void setUnderlyingItem(V obj) {
        this.value = obj;
    }

    /**
     * Gets the {@link SerializationGroupImpl} of which this object is a member.
     *
     * @return the group. May return <code>null</code>
     */
    @Override
    public SerializationGroup<K, V, G> getGroup() {
        return group;
    }

    /**
     * Sets the {@link SerializationGroupImpl} of which this object is a member.
     *
     * @param the group. May be <code>null</code>
     */
    @Override
    public void setGroup(SerializationGroup<K, V, G> group) {
        if (this.group != group) {
            // Remove any lock held on existing group
            if (group == null && groupLockHeld) {
                this.group.unlock();
                groupLockHeld = false;
            }

            this.group = group;

            if (this.groupId == null && group != null)
                this.groupId = group.getId();

            if (group != null && lock.isHeldByCurrentThread()) {
                group.lock();
                groupLockHeld = true;
            }
        }
    }

    /**
     * Gets the id for the group
     *
     * @return
     */
    @Override
    public G getGroupId() {
        return groupId;
    }

    @Override
    public void prePassivate() {
        // By the time we get here this thread has already locked the
        // group. It's possible another thread has locked this member
        // but not yet the group. We're willing to wait 2ms for that
        // other thread to see it can't lock the group and release this
        // member's lock, which it will only do if it is another
        // passivation thread. A request thread will hold the lock and
        // we'll fail (as intended)
        if (tryLock(2)) {
            try {
                // make sure we don't passivate the group twice
                // use the setter to clear any group lock
                setGroup(null);

                cache.passivate(this.id);
            } finally {
                unlock();
            }
        } else {
            throw EjbMessages.MESSAGES.cacheEntryInUse(this);
        }

    }

    @Override
    public void preReplicate() {
        // By the time we get here this thread has already locked the
        // group. It's possible another thread has locked this member
        // but not yet the group. We're willing to wait 2ms for that
        // other thread to see it can't lock the group and release this
        // member's lock, which it will only do if it is a passivation thread
        // A request thread will hold the lock and we'll fail (as intended)
        if (tryLock(2)) {
            try {
                // make sure we don't passivate the group twice
                // use the setter to clear any group lock
                setGroup(null);

                cache.notifyPreReplicate(this);
            } finally {
                unlock();
            }
        } else {
            throw EjbMessages.MESSAGES.cacheEntryInUse(this);
        }
    }

    /**
     * Notification that the group has been activated from a passivated state.
     */
    public void postActivate() {
    }

    @Override
    public boolean isPreReplicated() {
        return preReplicated;
    }

    @Override
    public void setPreReplicated(boolean preReplicated) {
        this.preReplicated = preReplicated;
    }

    /**
     * Notification that the previously replicated group has been retrieved from a clustered cache.
     */
    public void postReplicate() {
    }

    @Override
    public void setInUse(boolean inUse) {
        super.setInUse(inUse);

        // Tell our group about it
        if (group != null) {
            boolean localGroupLock = false;
            if (!groupLockHeld) {
                group.lock();
                localGroupLock = groupLockHeld = true;
            }
            try {
                if (inUse)
                    group.addInUse(id);
                else
                    group.removeInUse(id);
            } finally {
                if (localGroupLock) {
                    group.unlock();
                    groupLockHeld = false;
                }
            }
        }
    }

    /**
     * Allows our controlling {@link PassivatingBackingCache} to provide us a reference after deserialization.
     *
     * @param delegate
     */
    @Override
    public void setPassivatingCache(GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> delegate) {
        this.cache = delegate;
    }

    @Override
    public void lock() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw EjbMessages.MESSAGES.lockAcquisitionInterrupted(e, this);
        }

        if (!groupLockHeld && group != null) {
            try {
                group.lock();
                groupLockHeld = true;
            } finally {
                if (!groupLockHeld)
                    lock.unlock();
            }
        }
    }

    @Override
    public boolean tryLock() {
        boolean success = lock.tryLock();
        if (success) {
            success = (groupLockHeld || group == null);
            if (!success) {
                try {
                    success = groupLockHeld = group.tryLock();
                    if (!success) {
                        log.tracef("Member %s cannot lock serialization group %s", id, groupId);
                    }
                } finally {
                    if (!success)
                        lock.unlock();
                }
            }
        }
        return success;
    }

    private boolean tryLock(long wait) {
        boolean success = false;
        try {
            success = lock.tryLock(wait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // success remains false
        }
        if (success) {
            success = (groupLockHeld || group == null);
            if (!success) {
                try {
                    success = groupLockHeld = group.tryLock();
                    if (!success) {
                        log.tracef("Member %s cannot lock serialization group %s", id, groupId);
                    }
                } finally {
                    if (!success)
                        lock.unlock();
                }
            }
        }
        return success;

    }

    @Override
    public void unlock() {
        if (groupLockHeld && lock.getHoldCount() == 1) {
            // time to release our group lock
            group.unlock();
            groupLockHeld = false;
        }
        lock.unlock();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        lock = new ReentrantLock();
        if (groupId == null) {
            this.value = (V) in.readObject();
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (groupId != null) {
            setGroup(null);
        }
        out.defaultWriteObject();
        if (groupId == null) {
            out.writeObject(value);
        }
    }
}
