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

package org.jboss.as.controller.registry;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * A HashMap that is optimized for fast shallow copies. If the copy-ctor is
 * passed another FastCopyHashMap, or clone is called on this map, the shallow
 * copy can be performed using little more than a single array copy. In order to
 * accomplish this, immutable objects must be used internally, so update
 * operations result in slightly more object churn than <code>HashMap</code>.
 * <p/>
 * Note: It is very important to use a smaller load factor than you normally
 * would for HashMap, since the implementation is open-addressed with linear
 * probing. With a 50% load-factor a get is expected to return in only 2 probes.
 * However, a 90% load-factor is expected to return in around 50 probes.
 *
 * @author Jason T. Greene
 */
class FastCopyHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    /**
     * Marks null keys.
     */
    private static final Object NULL = new Object();

    /**
     * Serialization ID
     */
    private static final long serialVersionUID = 10929568968762L;

    /**
     * Same default as HashMap, must be a power of 2
     */
    private static final int DEFAULT_CAPACITY = 8;

    /**
     * MAX_INT - 1
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 67%, just like IdentityHashMap
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.67f;

    /**
     * The open-addressed table
     */
    private transient Entry<K, V>[] table;

    /**
     * The current number of key-value pairs
     */
    private transient int size;

    /**
     * The next resize
     */
    private transient int threshold;

    /**
     * The user defined load factor which defines when to resize
     */
    private final float loadFactor;

    /**
     * Counter used to detect changes made outside of an iterator
     */
    private transient int modCount;

    // Cached views
    private transient KeySet keySet;
    private transient Values values;
    private transient EntrySet entrySet;

    public FastCopyHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw MESSAGES.invalidTableSize();

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (!(loadFactor > 0F && loadFactor <= 1F))
            throw MESSAGES.invalidLoadFactor();

        this.loadFactor = loadFactor;
        init(initialCapacity, loadFactor);
    }

    @SuppressWarnings("unchecked")
    public FastCopyHashMap(Map<? extends K, ? extends V> map) {
        if (map instanceof FastCopyHashMap) {
            FastCopyHashMap<? extends K, ? extends V> fast = (FastCopyHashMap<? extends K, ? extends V>) map;
            this.table = (Entry<K, V>[]) fast.table.clone();
            this.loadFactor = fast.loadFactor;
            this.size = fast.size;
            this.threshold = fast.threshold;
        } else {
            this.loadFactor = DEFAULT_LOAD_FACTOR;
            init(map.size(), this.loadFactor);
            putAll(map);
        }
    }

    @SuppressWarnings("unchecked")
    private void init(int initialCapacity, float loadFactor) {
        int c = 1;
        while (c < initialCapacity) c <<= 1;
        threshold = (int) (c * loadFactor);

        // Include the load factor when sizing the table for the first time
        if (initialCapacity > threshold && c < MAXIMUM_CAPACITY) {
            c <<= 1;
            threshold = (int) (c * loadFactor);
        }

        this.table = (Entry<K, V>[]) new Entry[c];
    }

    public FastCopyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public FastCopyHashMap() {
        this(DEFAULT_CAPACITY);
    }

    // The normal bit spreader...

    private static int hash(Object key) {
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    @SuppressWarnings("unchecked")
    private static <K> K maskNull(K key) {
        return key == null ? (K) NULL : key;
    }

    private static <K> K unmaskNull(K key) {
        return key == NULL ? null : key;
    }

    private int nextIndex(int index, int length) {
        index = (index >= length - 1) ? 0 : index + 1;
        return index;
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }

    private static int index(int hashCode, int length) {
        return hashCode & (length - 1);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V get(Object key) {
        key = maskNull(key);

        int hash = hash(key);
        int length = table.length;
        int index = index(hash, length);

        for (int start = index; ;) {
            Entry<K, V> e = table[index];
            if (e == null)
                return null;

            if (e.hash == hash && eq(key, e.key))
                return e.value;

            index = nextIndex(index, length);
            if (index == start) // Full table
                return null;
        }
    }

    public boolean containsKey(Object key) {
        key = maskNull(key);

        int hash = hash(key);
        int length = table.length;
        int index = index(hash, length);

        for (int start = index; ;) {
            Entry<K, V> e = table[index];
            if (e == null)
                return false;

            if (e.hash == hash && eq(key, e.key))
                return true;

            index = nextIndex(index, length);
            if (index == start) // Full table
                return false;
        }
    }

    public boolean containsValue(Object value) {
        for (Entry<K, V> e : table)
            if (e != null && eq(value, e.value))
                return true;

        return false;
    }

    public V put(K key, V value) {
        key = maskNull(key);

        Entry<K, V>[] table = this.table;
        int hash = hash(key);
        int length = table.length;
        int index = index(hash, length);

        for (int start = index; ;) {
            Entry<K, V> e = table[index];
            if (e == null)
                break;

            if (e.hash == hash && eq(key, e.key)) {
                table[index] = new Entry<K, V>(e.key, e.hash, value);
                return e.value;
            }

            index = nextIndex(index, length);
            if (index == start)
                throw MESSAGES.tableIsFull();
        }

        modCount++;
        table[index] = new Entry<K, V>(key, hash, value);
        if (++size >= threshold)
            resize(length);

        return null;
    }


    @SuppressWarnings("unchecked")
    private void resize(int from) {
        int newLength = from << 1;

        // Can't get any bigger
        if (newLength > MAXIMUM_CAPACITY || newLength <= from)
            return;

        Entry<K, V>[] newTable = new Entry[newLength];
        Entry<K, V>[] old = table;

        for (Entry<K, V> e : old) {
            if (e == null)
                continue;

            int index = index(e.hash, newLength);
            while (newTable[index] != null)
                index = nextIndex(index, newLength);

            newTable[index] = e;
        }

        threshold = (int) (loadFactor * newLength);
        table = newTable;
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        int size = map.size();
        if (size == 0)
            return;

        if (size > threshold) {
            if (size > MAXIMUM_CAPACITY)
                size = MAXIMUM_CAPACITY;

            int length = table.length;
            while (length < size) length <<= 1;

            resize(length);
        }

        for (Map.Entry<? extends K, ? extends V> e : map.entrySet())
            put(e.getKey(), e.getValue());
    }

    public V remove(Object key) {
        key = maskNull(key);

        Entry<K, V>[] table = this.table;
        int length = table.length;
        int hash = hash(key);
        int start = index(hash, length);

        for (int index = start; ;) {
            Entry<K, V> e = table[index];
            if (e == null)
                return null;

            if (e.hash == hash && eq(key, e.key)) {
                table[index] = null;
                relocate(index);
                modCount++;
                size--;
                return e.value;
            }

            index = nextIndex(index, length);
            if (index == start)
                return null;
        }


    }

    private void relocate(int start) {
        Entry<K, V>[] table = this.table;
        int length = table.length;
        int current = nextIndex(start, length);

        for (; ;) {
            Entry<K, V> e = table[current];
            if (e == null)
                return;

            // A Doug Lea variant of Knuth's Section 6.4 Algorithm R.
            // This provides a non-recursive method of relocating
            // entries to their optimal positions once a gap is created.
            int prefer = index(e.hash, length);
            if ((current < prefer && (prefer <= start || start <= current))
                    || (prefer <= start && start <= current)) {
                table[start] = e;
                table[current] = null;
                start = current;
            }

            current = nextIndex(current, length);
        }
    }

    public void clear() {
        modCount++;
        Entry<K, V>[] table = this.table;
        for (int i = 0; i < table.length; i++)
            table[i] = null;

        size = 0;
    }

    @SuppressWarnings("unchecked")
    public FastCopyHashMap<K, V> clone() {
        try {
            FastCopyHashMap<K, V> clone = (FastCopyHashMap<K, V>) super.clone();
            clone.table = table.clone();
            clone.entrySet = null;
            clone.values = null;
            clone.keySet = null;
            return clone;
        }
        catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
    }

    public void printDebugStats() {
        int optimal = 0;
        int total = 0;
        int totalSkew = 0;
        int maxSkew = 0;
        for (int i = 0; i < table.length; i++) {
            Entry<K, V> e = table[i];
            if (e != null) {

                total++;
                int target = index(e.hash, table.length);
                if (i == target)
                    optimal++;
                else {
                    int skew = Math.abs(i - target);
                    if (skew > maxSkew) maxSkew = skew;
                    totalSkew += skew;
                }

            }
        }

        System.out.println(" Size:            " + size);
        System.out.println(" Real Size:       " + total);
        System.out.println(" Optimal:         " + optimal + " (" + (float) optimal * 100 / total + "%)");
        System.out.println(" Average Distnce: " + ((float) totalSkew / (total - optimal)));
        System.out.println(" Max Distance:    " + maxSkew);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null)
            entrySet = new EntrySet();

        return entrySet;
    }

    public Set<K> keySet() {
        if (keySet == null)
            keySet = new KeySet();

        return keySet;
    }

    public Collection<V> values() {
        if (values == null)
            values = new Values();

        return values;
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        int size = s.readInt();

        init(size, loadFactor);

        for (int i = 0; i < size; i++) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            putForCreate(key, value);
        }

        this.size = size;
    }

    @SuppressWarnings("unchecked")
    private void putForCreate(K key, V value) {
        key = maskNull(key);

        Entry<K, V>[] table = this.table;
        int hash = hash(key);
        int length = table.length;
        int index = index(hash, length);

        Entry<K, V> e = table[index];
        while (e != null) {
            index = nextIndex(index, length);
            e = table[index];
        }

        table[index] = new Entry<K, V>(key, hash, value);
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(size);

        for (Entry<K, V> e : table) {
            if (e != null) {
                s.writeObject(unmaskNull(e.key));
                s.writeObject(e.value);
            }
        }
    }

    private static final class Entry<K, V> {
        final K key;
        final int hash;
        final V value;

        Entry(K key, int hash, V value) {
            this.key = key;
            this.hash = hash;
            this.value = value;
        }
    }

    private abstract class FasyCopyHashMapIterator<E> implements Iterator<E> {
        private int next = 0;
        private int expectedCount = modCount;
        private int current = -1;
        private boolean hasNext;
        Entry<K, V>[] table = FastCopyHashMap.this.table;

        public boolean hasNext() {
            if (hasNext == true)
                return true;

            Entry<K, V>[] table = this.table;
            for (int i = next; i < table.length; i++) {
                if (table[i] != null) {
                    next = i;
                    return hasNext = true;
                }
            }

            next = table.length;
            return false;
        }

        protected Entry<K, V> nextEntry() {
            if (modCount != expectedCount)
                throw new ConcurrentModificationException();

            if (!hasNext && !hasNext())
                throw new NoSuchElementException();

            current = next++;
            hasNext = false;

            return table[current];
        }

        @SuppressWarnings("unchecked")
        public void remove() {
            if (modCount != expectedCount)
                throw new ConcurrentModificationException();

            int current = this.current;
            int delete = current;

            if (current == -1)
                throw new IllegalStateException();

            // Invalidate current (prevents multiple remove)
            this.current = -1;

            // Start were we relocate
            next = delete;

            Entry<K, V>[] table = this.table;
            if (table != FastCopyHashMap.this.table) {
                FastCopyHashMap.this.remove(table[delete].key);
                table[delete] = null;
                expectedCount = modCount;
                return;
            }


            int length = table.length;
            int i = delete;

            table[delete] = null;
            size--;

            for (; ;) {
                i = nextIndex(i, length);
                Entry<K, V> e = table[i];
                if (e == null)
                    break;

                int prefer = index(e.hash, length);
                if ((i < prefer && (prefer <= delete || delete <= i))
                        || (prefer <= delete && delete <= i)) {
                    // Snapshot the unseen portion of the table if we have
                    // to relocate an entry that was already seen by this iterator
                    if (i < current && current <= delete && table == FastCopyHashMap.this.table) {
                        int remaining = length - current;
                        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[remaining];
                        System.arraycopy(table, current, newTable, 0, remaining);

                        // Replace iterator's table.
                        // Leave table local var pointing to the real table
                        this.table = newTable;
                        next = 0;
                    }

                    // Do the swap on the real table
                    table[delete] = e;
                    table[i] = null;
                    delete = i;
                }
            }
        }
    }


    private class KeyIterator extends FasyCopyHashMapIterator<K> {
        public K next() {
            return unmaskNull(nextEntry().key);
        }
    }

    private class ValueIterator extends FasyCopyHashMapIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private class EntryIterator extends FasyCopyHashMapIterator<Map.Entry<K, V>> {
        private class WriteThroughEntry extends SimpleEntry<K, V> {
            WriteThroughEntry(K key, V value) {
                super(key, value);
            }

            public V setValue(V value) {
                if (table != FastCopyHashMap.this.table)
                    FastCopyHashMap.this.put(getKey(), value);

                return super.setValue(value);
            }
        }

        public Map.Entry<K, V> next() {
            Entry<K, V> e = nextEntry();
            return new WriteThroughEntry(unmaskNull(e.key), e.value);
        }

    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public void clear() {
            FastCopyHashMap.this.clear();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            int size = size();
            FastCopyHashMap.this.remove(o);
            return size() < size;
        }

        public int size() {
            return FastCopyHashMap.this.size();
        }
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public void clear() {
            FastCopyHashMap.this.clear();
        }

        public int size() {
            return FastCopyHashMap.this.size();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;

            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object value = get(entry.getKey());
            return eq(entry.getValue(), value);
        }

        public void clear() {
            FastCopyHashMap.this.clear();
        }

        public boolean isEmpty() {
            return FastCopyHashMap.this.isEmpty();
        }

        public int size() {
            return FastCopyHashMap.this.size();
        }
    }

    protected static class SimpleEntry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;

        SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        SimpleEntry(Map.Entry<K, V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : hash(key)) ^
                    (value == null ? 0 : hash(value));
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
