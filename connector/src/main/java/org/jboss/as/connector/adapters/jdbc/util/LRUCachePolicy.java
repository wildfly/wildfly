/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.adapters.jdbc.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a Least Recently Used cache policy.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class LRUCachePolicy {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------
    /**
     * The map holding the cached objects
     */
    protected Map map;

    /**
     * The linked list used to implement the LRU algorithm
     */
    protected LRUList lruList;

    /**
     * The maximum capacity of this cache
     */
    protected int maxCapacity;

    /**
     * The minimum capacity of this cache
     */
    protected int minCapacity;

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    /**
     * Creates a LRU cache policy object with zero cache capacity.
     *
     * @see #create
     */
    public LRUCachePolicy() {
    }

    /**
     * Creates a LRU cache policy object with the specified minimum
     * and maximum capacity.
     *
     * @param min min
     * @param max max
     * @see #create
     */
    public LRUCachePolicy(int min, int max) {
        if (min < 2 || min > max) {
            throw new IllegalArgumentException("Illegal cache capacities");
        }
        minCapacity = min;
        maxCapacity = max;
    }

    /**
     * Create map holding entries.
     *
     * @return the map
     */
    protected Map createMap() {
        return new HashMap();
    }

    // Public --------------------------------------------------------

    // Service implementation ----------------------------------------------

    /**
     * Initializes the cache, creating all required objects and initializing their
     * values.
     *
     * @see #start
     * @see #destroy
     */
    public void create() {
        map = createMap();
        lruList = createList();
        lruList.maxCapacity = maxCapacity;
        lruList.minCapacity = minCapacity;
        lruList.capacity = maxCapacity;
    }

    /**
     * Starts this cache that is now ready to be used.
     *
     * @see #create
     * @see #stop
     */
    public void start() {
    }

    /**
     * Stops this cache thus {@link #flush}ing all cached objects. <br>
     * After this method is called, a call to {@link #start} will restart the cache.
     *
     * @see #start
     * @see #destroy
     */
    public void stop() {
        if (lruList != null) {
            flush();
        }
    }

    /**
     * Destroys the cache that is now unusable. <br>
     * To have it working again it must be re-{@link #create}ed and
     * re-{@link #start}ed.
     *
     * @see #create
     */
    public void destroy() {
        if (map != null)
            map.clear();
        if (lruList != null)
            lruList.clear();
    }

    /**
     * get method
     *
     * @param key key
     * @return the value
     */
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Requesting an object using a null key");
        }

        LRUCacheEntry value = (LRUCacheEntry) map.get(key);
        if (value != null) {
            lruList.promote(value);
            return value.object;
        } else {
            cacheMiss();
            return null;
        }
    }

    /**
     * peek
     *
     * @param key the key
     * @return the value
     */
    public Object peek(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Requesting an object using a null key");
        }

        LRUCacheEntry value = (LRUCacheEntry) map.get(key);
        if (value == null) {
            return null;
        } else {
            return value.object;
        }
    }

    /**
     * insert
     *
     * @param key the key
     * @param o   value
     */
    public void insert(Object key, Object o) {
        if (o == null) {
            throw new IllegalArgumentException("Cannot insert a null object in the cache");
        }
        if (key == null) {
            throw new IllegalArgumentException("Cannot insert an object in the cache with null key");
        }
        if (map.containsKey(key)) {
            throw new IllegalStateException("Attempt to put in the cache an object that is already there");
        }
        lruList.demote();
        LRUCacheEntry entry = createCacheEntry(key, o);
        map.put(key, entry);
        lruList.promote(entry);
    }

    /**
     * remove
     *
     * @param key the key
     */
    public void remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Removing an object using a null key");
        }

        Object value = map.remove(key);
        if (value != null) {
            lruList.remove((LRUCacheEntry) value);
        }
        //else Do nothing, the object isn't in the cache list
    }

    /**
     * flush
     */
    public void flush() {
        LRUCacheEntry entry = null;
        while ((entry = lruList.tail) != null) {
            ageOut(entry);
        }
    }

    /**
     * size
     *
     * @return the size
     */
    public int size() {
        return lruList.count;
    }

    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    /**
     * Factory method for the linked list used by this cache implementation.
     *
     * @return the lru list
     */
    protected LRUList createList() {
        return new LRUList();
    }

    /**
     * Callback method called when the cache algorithm ages out of the cache
     * the given entry. <br>
     * The implementation here is removing the given entry from the cache.
     *
     * @param entry entry
     */
    protected void ageOut(LRUCacheEntry entry) {
        remove(entry.key);
    }

    /**
     * Callback method called when a cache miss happens.
     */
    protected void cacheMiss() {
    }

    /**
     * Factory method for cache entries
     *
     * @param key   key
     * @param value value
     * @return the entry entry
     */
    protected LRUCacheEntry createCacheEntry(Object key, Object value) {
        return new LRUCacheEntry(key, value);
    }

    // Private -------------------------------------------------------

    // Inner classes -------------------------------------------------

    /**
     * Double queued list used to store cache entries.
     */
    public class LRUList {
        /**
         * The maximum capacity of the cache list
         */
        private int maxCapacity;

        /**
         * The minimum capacity of the cache list
         */
        private int minCapacity;

        /**
         * The current capacity of the cache list
         */
        private int capacity;

        /**
         * The number of cached objects
         */
        private int count;

        /**
         * The head of the double linked list
         */
        private LRUCacheEntry head;

        /**
         * The tail of the double linked list
         */
        private LRUCacheEntry tail;

        /**
         * The cache misses happened
         */
        private int cacheMiss;

        /**
         * Creates a new double queued list.
         */
        protected LRUList() {
            head = null;
            tail = null;
            count = 0;
        }

        /**
         * Promotes the cache entry <code>entry</code> to the last used position
         * of the list. <br>
         * If the object is already there, does nothing.
         *
         * @param entry the object to be promoted, cannot be null
         * @throws IllegalStateException if this method is called with a full cache
         * @see #demote
         */
        protected void promote(LRUCacheEntry entry) {
            if (entry == null) {
                throw new IllegalArgumentException("Trying to promote a null object");
            }
            if (capacity < 1) {
                throw new IllegalStateException("Can't work with capacity < 1");
            }

            entryPromotion(entry);

            entry.time = System.currentTimeMillis();
            if (entry.prev == null) {
                if (entry.next == null) {
                    // entry is new or there is only the head
                    if (count == 0) { // cache is empty
                        head = entry;
                        tail = entry;
                        ++count;
                        entryAdded(entry);
                    } else if (count == 1 && head == entry) {
                        // there is only the head and I want to promote it, do nothing
                    } else if (count < capacity) {
                        entry.prev = null;
                        entry.next = head;
                        head.prev = entry;
                        head = entry;
                        ++count;
                        entryAdded(entry);
                    } else if (count < maxCapacity) {
                        entry.prev = null;
                        entry.next = head;
                        head.prev = entry;
                        head = entry;
                        ++count;
                        int oldCapacity = capacity;
                        ++capacity;
                        entryAdded(entry);
                        capacityChanged(oldCapacity);
                    } else {
                        throw new IllegalStateException("Attempt to put a new cache entry on a full cache");
                    }
                } else {
                    // entry is the head, do nothing
                }
            } else {
                if (entry.next == null) {// entry is the tail
                    LRUCacheEntry beforeLast = entry.prev;
                    beforeLast.next = null;
                    entry.prev = null;
                    entry.next = head;
                    head.prev = entry;
                    head = entry;
                    tail = beforeLast;
                } else {
                // entry is in the middle of the list
                    LRUCacheEntry previous = entry.prev;
                    previous.next = entry.next;
                    entry.next.prev = previous;
                    entry.prev = null;
                    entry.next = head;
                    head.prev = entry;
                    head = entry;
                }
            }
        }

        /**
         * Demotes from the cache the least used entry. <br>
         * If the cache is not full, does nothing.
         *
         * @see #promote
         */
        protected void demote() {
            if (capacity < 1) {
                throw new IllegalStateException("Can't work with capacity < 1");
            }
            if (count > maxCapacity) {
                throw new IllegalStateException("Cache list entries number (" + count +
                        ") > than the maximum allowed (" + maxCapacity + ")");
            }
            if (count == maxCapacity) {
                LRUCacheEntry entry = tail;

                // the entry will be removed by ageOut
                ageOut(entry);
            } else {
                // cache is not full, do nothing
            }
        }

        /**
         * Removes from the cache list the specified entry.
         *
         * @param entry entry
         */
        protected void remove(LRUCacheEntry entry) {
            if (entry == null) {
                throw new IllegalArgumentException("Cannot remove a null entry from the cache");
            }
            if (count < 1) {
                throw new IllegalStateException("Trying to remove an entry from an empty cache");
            }

            entry.key = null;
            entry.object = null;
            if (count == 1) {
                head = null;
                tail = null;
            } else {
                if (entry.prev == null) { // the head
                    head = entry.next;
                    head.prev = null;
                    entry.next = null;
                } else if (entry.next == null) { // the tail
                    tail = entry.prev;
                    tail.next = null;
                    entry.prev = null;
                } else {
                // in the middle
                    entry.next.prev = entry.prev;
                    entry.prev.next = entry.next;
                    entry.prev = null;
                    entry.next = null;
                }
            }
            --count;
            entryRemoved(entry);
        }

        /**
         * Callback that signals that the given entry is just about to be added.
         *
         * @param entry entry
         */
        protected void entryPromotion(LRUCacheEntry entry) {
        }

        /**
         * Callback that signals that the given entry has been added to the cache.
         *
         * @param entry entry
         */
        protected void entryAdded(LRUCacheEntry entry) {
        }

        /**
         * Callback that signals that the given entry has been removed from the cache.
         *
         * @param entry entry
         */
        protected void entryRemoved(LRUCacheEntry entry) {
        }

        /**
         * Callback that signals that the capacity of the cache is changed.
         *
         * @param oldCapacity the capacity before the change happened
         */
        protected void capacityChanged(int oldCapacity) {
        }

        /**
         * clear
         */
        protected void clear() {
            LRUCacheEntry entry = head;
            head = null;
            tail = null;
            count = 0;
            for (; entry != null; entry = entry.next)
                entryRemoved(entry);
        }

        @Override
        public String toString() {
            String s = Integer.toHexString(super.hashCode());
            s += " size: " + count;
            for (LRUCacheEntry entry = head; entry != null; entry = entry.next) {
                s += "\n" + entry;
            }
            return s;
        }

        /**
         * Get the maxCapacity.
         *
         * @return the maxCapacity.
         */
        public final int getMaxCapacity() {
            return maxCapacity;
        }

        /**
         * Set the maxCapacity.
         *
         * @param maxCapacity The maxCapacity to set.
         */
        public final void setMaxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        /**
         * Get the minCapacity.
         *
         * @return the minCapacity.
         */
        public final int getMinCapacity() {
            return minCapacity;
        }

        /**
         * Set the minCapacity.
         *
         * @param minCapacity The minCapacity to set.
         */
        public final void setMinCapacity(int minCapacity) {
            this.minCapacity = minCapacity;
        }

        /**
         * Get the capacity.
         *
         * @return the capacity.
         */
        public final int getCapacity() {
            return capacity;
        }

        /**
         * Set the capacity.
         *
         * @param capacity The capacity to set.
         */
        public final void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        /**
         * Get the count.
         *
         * @return the count.
         */
        public final int getCount() {
            return count;
        }

        /**
         * Set the count.
         *
         * @param count The count to set.
         */
        public final void setCount(int count) {
            this.count = count;
        }

        /**
         * Get the head.
         *
         * @return the head.
         */
        public final LRUCacheEntry getHead() {
            return head;
        }

        /**
         * Set the head.
         *
         * @param head The head to set.
         */
        public final void setHead(LRUCacheEntry head) {
            this.head = head;
        }

        /**
         * Get the tail.
         *
         * @return the tail.
         */
        public final LRUCacheEntry getTail() {
            return tail;
        }

        /**
         * Set the tail.
         *
         * @param tail The tail to set.
         */
        public final void setTail(LRUCacheEntry tail) {
            this.tail = tail;
        }

        /**
         * Get the cacheMiss.
         *
         * @return the cacheMiss.
         */
        public final int getCacheMiss() {
            return cacheMiss;
        }

        /**
         * Set the cacheMiss.
         *
         * @param cacheMiss The cacheMiss to set.
         */
        public final void setCacheMiss(int cacheMiss) {
            this.cacheMiss = cacheMiss;
        }
    }

    /**
     * Double linked cell used as entry in the cache list.
     */
    public class LRUCacheEntry {
        /**
         * Reference to the next cell in the list
         */
        private LRUCacheEntry next;

        /**
         * Reference to the previous cell in the list
         */
        private LRUCacheEntry prev;

        /**
         * The key used to retrieve the cached object
         */
        private Object key;

        /**
         * The cached object
         */
        private Object object;

        /**
         * The timestamp of the creation
         */
        private long time;

        /**
         * Creates a new double linked cell, storing the object we
         * want to cache and the key that is used to retrieve it.
         *
         * @param key    key
         * @param object object
         */
        protected LRUCacheEntry(Object key, Object object) {
            this.key = key;
            this.object = object;
            next = null;
            prev = null;
            time = 0; // Set when inserted in the list.
        }

        @Override
        public String toString() {
            return "key: " + key + ", object: " +
                    (object == null ? "null" : Integer.toHexString(object.hashCode())) + ", entry: " +
                    Integer.toHexString(super.hashCode());
        }

        /**
         * Get the next.
         *
         * @return the next.
         */
        public final LRUCacheEntry getNext() {
            return next;
        }

        /**
         * Set the next.
         *
         * @param next The next to set.
         */
        public final void setNext(LRUCacheEntry next) {
            this.next = next;
        }

        /**
         * Get the prev.
         *
         * @return the prev.
         */
        public final LRUCacheEntry getPrev() {
            return prev;
        }

        /**
         * Set the prev.
         *
         * @param prev The prev to set.
         */
        public final void setPrev(LRUCacheEntry prev) {
            this.prev = prev;
        }

        /**
         * Get the key.
         *
         * @return the key.
         */
        public final Object getKey() {
            return key;
        }

        /**
         * Set the key.
         *
         * @param key The key to set.
         */
        public final void setKey(Object key) {
            this.key = key;
        }

        /**
         * Get the object.
         *
         * @return the object.
         */
        public final Object getObject() {
            return object;
        }

        /**
         * Set the object.
         *
         * @param object The object to set.
         */
        public final void setObject(Object object) {
            this.object = object;
        }

        /**
         * Get the time.
         *
         * @return the time.
         */
        public final long getTime() {
            return time;
        }

        /**
         * Set the time.
         *
         * @param time The time to set.
         */
        public final void setTime(long time) {
            this.time = time;
        }
    }
}
