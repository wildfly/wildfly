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
package org.jboss.as.cmp.jdbc2.schema;

import javax.transaction.Transaction;
import java.util.Map;
import java.util.HashMap;
import org.jboss.as.cmp.CmpMessages;


/**
 * Simple LRU cache. Items are evicted when maxCapacity is exceeded.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 89152 $</tt>
 * @jmx:mbean extends="org.jboss.system.ServiceMBean"
 */
public class TableCache implements Cache {
    private Cache.Listener listener = Cache.Listener.NOOP;
    private final Map rowsById;
    private CachedRow head;
    private CachedRow tail;
    private int maxCapacity;
    private final int minCapacity;

    private boolean locked;

    private final int partitionIndex;

    public TableCache(int partitionIndex, int initialCapacity, int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.minCapacity = initialCapacity;
        rowsById = new HashMap(initialCapacity);
        this.partitionIndex = partitionIndex;
    }

    /**
     * @jmx.managed-operation
     */
    public void registerListener(Cache.Listener listener) {
        this.listener = listener;
    }

    /**
     * @jmx.managed-operation
     */
    public int size() {
        lock();
        try {
            return rowsById.size();
        } finally {
            unlock();
        }
    }

    /**
     * @jmx.managed-attribute
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @jmx.managed-attribute
     */
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * @jmx.managed-attribute
     */
    public int getMinCapacity() {
        return minCapacity;
    }

    public synchronized void lock() {
        boolean intr = false;
        try {
            if (locked) {
                long start = System.currentTimeMillis();
                while (locked) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }

                listener.contention(partitionIndex, System.currentTimeMillis() - start);
            }
            locked = true;
        } finally {
            if (intr) Thread.currentThread().interrupt();
        }
    }

    public void lock(Object key) {
        lock();
    }

    public synchronized void unlock() {
        if (!locked) {
            throw CmpMessages.MESSAGES.instanceIsLocked();
        }
        locked = false;
        notify();
    }

    public void unlock(Object key) {
        unlock();
    }

    public Object[] getFields(Object pk) {
        Object[] fields;
        CachedRow row = (CachedRow) rowsById.get(pk);
        if (row != null && row.locker == null) {
            promoteRow(row);
            fields = new Object[row.fields.length];
            System.arraycopy(row.fields, 0, fields, 0, fields.length);
            listener.hit(partitionIndex);
        } else {
            fields = null;
            listener.miss(partitionIndex);
        }
        return fields;
    }

    public Object[] getRelations(Object pk) {
        Object[] relations;
        CachedRow row = (CachedRow) rowsById.get(pk);
        if (row != null && row.relations != null && row.locker == null) {
            promoteRow(row);
            relations = new Object[row.relations.length];
            System.arraycopy(row.relations, 0, relations, 0, relations.length);
        } else {
            relations = null;
        }
        return relations;
    }

    public void put(Transaction tx, Object pk, Object[] fields, Object[] relations) {
        CachedRow row = (CachedRow) rowsById.get(pk);
        if (row == null) { // the row is not cached
            Object[] fieldsCopy = new Object[fields.length];
            System.arraycopy(fields, 0, fieldsCopy, 0, fields.length);
            row = new CachedRow(pk, fieldsCopy);

            if (relations != null) {
                Object[] relationsCopy = new Object[relations.length];
                System.arraycopy(relations, 0, relationsCopy, 0, relations.length);
                row.relations = relationsCopy;
            }

            rowsById.put(pk, row);

            if (head == null) {
                head = row;
                tail = row;
            } else {
                head.prev = row;
                row.next = head;
                head = row;
            }
        } else if (row.locker == null || row.locker.equals(tx)) { // the row is cached
            promoteRow(row);
            System.arraycopy(fields, 0, row.fields, 0, fields.length);

            if (relations != null) {
                if (row.relations == null) {
                    row.relations = new Object[relations.length];
                }
                System.arraycopy(relations, 0, row.relations, 0, relations.length);
            }

            row.lastUpdated = System.currentTimeMillis();
            row.locker = null;
        }

        CachedRow victim = tail;
        while (rowsById.size() > maxCapacity && victim != null) {
            CachedRow nextVictim = victim.prev;
            if (victim.locker == null) {
                dereference(victim);
                rowsById.remove(victim.pk);
                listener.eviction(partitionIndex, row.pk, rowsById.size());
            }
            victim = nextVictim;
        }
    }

    public void ageOut(long lastUpdated) {
        CachedRow victim = tail;
        while (victim != null && victim.lastUpdated < lastUpdated) {
            CachedRow nextVictim = victim.prev;
            if (victim.locker == null) {
                dereference(victim);
                rowsById.remove(victim.pk);
                listener.eviction(partitionIndex, victim.pk, rowsById.size());
            }
            victim = nextVictim;
        }
    }

    public void remove(Transaction tx, Object pk) {
        CachedRow row = (CachedRow) rowsById.remove(pk);
        if (row == null || row.locker != null && !tx.equals(row.locker)) {
            if(row == null) {
                throw CmpMessages.MESSAGES.removeRejected(pk, tx);
            } else {
                throw CmpMessages.MESSAGES.removeRejected(pk, tx, row.locker);
            }
        }

        dereference(row);
        row.locker = null;
    }

    public boolean contains(Transaction tx, Object pk) {
        CachedRow row = (CachedRow) rowsById.get(pk);
        return row != null && (row.locker == null || tx.equals(row.locker));
    }

    public void lockForUpdate(Transaction tx, Object pk) throws Exception {
        CachedRow row = (CachedRow) rowsById.get(pk);
        if (row != null) {
            if (row.locker != null && !tx.equals(row.locker)) {
                throw CmpMessages.MESSAGES.lockAcquisitionRejected(tx, row.locker, pk);
            }
            row.locker = tx;
        }
        // else?!
    }

    public void releaseLock(Transaction tx, Object pk) throws Exception {
        CachedRow row = (CachedRow) rowsById.get(pk);
        if (row != null) {
            if (!tx.equals(row.locker)) {
                throw CmpMessages.MESSAGES.lockReleaseRejected(tx, row.locker, pk);
            }
            row.locker = null;
        }
        // else?!
    }

    public void flush() {
        this.rowsById.clear();
        this.head = null;
        this.tail = null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('[');

        try {
            lock();

            CachedRow cursor = head;
            while (cursor != null) {
                buf.append('(')
                        .append(cursor.pk)
                        .append('|');

                for (int i = 0; i < cursor.fields.length; ++i) {
                    if (i > 0) {
                        buf.append(',');
                    }

                    buf.append(cursor.fields[i]);
                }

                buf.append(')');

                cursor = cursor.next;
            }
        } finally {
            unlock();
        }

        buf.append(']');
        return buf.toString();
    }

    // Private

    private void dereference(CachedRow row) {
        CachedRow next = row.next;
        CachedRow prev = row.prev;

        if (row == head) {
            head = next;
        }

        if (row == tail) {
            tail = prev;
        }

        if (next != null) {
            next.prev = prev;
        }

        if (prev != null) {
            prev.next = next;
        }

        row.next = null;
        row.prev = null;
    }

    private void promoteRow(CachedRow row) {
        if (head == null) { // this is the first row in the cache
            head = row;
            tail = row;
        } else if (row == head) { // this is the head
        } else if (row == tail) { // this is the tail
            tail = row.prev;
            tail.next = null;

            row.prev = null;
            row.next = head;

            head.prev = row;
            head = row;
        } else { // somewhere in the middle
            CachedRow next = row.next;
            CachedRow prev = row.prev;

            if (prev != null) {
                prev.next = next;
            }

            if (next != null) {
                next.prev = prev;
            }

            head.prev = row;
            row.next = head;
            row.prev = null;
            head = row;
        }
    }

    private class CachedRow {
        public final Object pk;
        public final Object[] fields;
        public Object[] relations;
        private Transaction locker;

        private CachedRow next;
        private CachedRow prev;

        public long lastUpdated = System.currentTimeMillis();

        public CachedRow(Object pk, Object[] fields) {
            this.pk = pk;
            this.fields = fields;
        }
    }
}
