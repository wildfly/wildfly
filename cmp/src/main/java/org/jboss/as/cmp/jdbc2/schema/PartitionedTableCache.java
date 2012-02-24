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

import org.jboss.logging.Logger;

import javax.transaction.Transaction;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 89152 $</tt>
 * @jmx:mbean extends="org.jboss.system.ServiceMBean"
 */
public class PartitionedTableCache implements Cache {
    private static final Logger log = Logger.getLogger(PartitionedTableCache.class);
    private Cache.Listener listener = Cache.Listener.NOOP;

    private final int minCapacity;
    private final int minPartitionCapacity;
    private int maxCapacity;
    private int maxPartitionCapacity;

    private final TableCache[] partitions;

    private Overager overager;

    public PartitionedTableCache(int minCapacity, int maxCapacity, int partitionsTotal) {
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;

        minPartitionCapacity = minCapacity / partitionsTotal + 1;
        maxPartitionCapacity = maxCapacity / partitionsTotal + 1;
        partitions = new TableCache[partitionsTotal];
        for (int i = 0; i < partitions.length; ++i) {
            partitions[i] = new TableCache(i, minPartitionCapacity, maxPartitionCapacity);
        }

        if (log.isTraceEnabled()) {
            log.trace("min-capacity=" + minCapacity + ", max-capacity=" + maxCapacity + ", partitions=" + partitionsTotal);
        }
    }

    public void stopService() {
        if (overager != null) {
            overager.stop();
        }
    }

    public void initOverager(long period, long maxAge, String threadName) {
        final long periodMs = period * 1000;
        final long maxAgeMs = maxAge * 1000;
        overager = new Overager(maxAgeMs, periodMs);
        new Thread(overager, threadName).start();
    }

    /**
     * @jmx.managed-operation
     */
    public void registerListener(Cache.Listener listener) {
        this.listener = listener;
        for (int i = 0; i < partitions.length; ++i) {
            partitions[i].registerListener(listener);
        }
    }

    /**
     * @jmx.managed-operation
     */
    public int size() {
        int size = 0;
        for (int i = 0; i < partitions.length; ++i) {
            size += partitions[i].size();
        }
        return size;
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
        this.maxPartitionCapacity = maxCapacity / partitions.length + 1;
        for (int i = 0; i < partitions.length; ++i) {
            partitions[i].setMaxCapacity(maxPartitionCapacity);
        }
    }

    /**
     * @jmx.managed-attribute
     */
    public int getMinCapacity() {
        return minCapacity;
    }

    /**
     * @jmx.managed-attribute
     */
    public int getPartitionsTotal() {
        return partitions.length;
    }

    /**
     * @jmx.managed-attribute
     */
    public int getMinPartitionCapacity() {
        return minPartitionCapacity;
    }

    /**
     * @jmx.managed-attribute
     */
    public int getMaxPartitionCapacity() {
        return maxPartitionCapacity;
    }

    public void lock() {
    }

    public void lock(Object key) {
        int partitionIndex = getPartitionIndex(key);
        partitions[partitionIndex].lock(key);
    }

    public void unlock() {
    }

    public void unlock(Object key) {
        int partitionIndex = getPartitionIndex(key);
        partitions[partitionIndex].unlock(key);
    }

    public Object[] getFields(Object pk) {
        final int i = getPartitionIndex(pk);
        return partitions[i].getFields(pk);
    }

    public Object[] getRelations(Object pk) {
        final int i = getPartitionIndex(pk);
        return partitions[i].getRelations(pk);
    }

    public void put(Transaction tx, Object pk, Object[] fields, Object[] relations) {
        final int i = getPartitionIndex(pk);
        partitions[i].put(tx, pk, fields, relations);
    }

    public void remove(Transaction tx, Object pk) {
        final int i = getPartitionIndex(pk);
        partitions[i].remove(tx, pk);
    }

    public boolean contains(Transaction tx, Object pk) {
        final int i = getPartitionIndex(pk);
        return partitions[i].contains(tx, pk);
    }

    public void lockForUpdate(Transaction tx, Object pk) throws Exception {
        final int i = getPartitionIndex(pk);
        partitions[i].lockForUpdate(tx, pk);
    }

    public void releaseLock(Transaction tx, Object pk) throws Exception {
        final int i = getPartitionIndex(pk);
        partitions[i].releaseLock(tx, pk);
    }

    public void flush() {
        for (int i = 0; i < partitions.length; ++i) {
            final TableCache partition = partitions[i];
            partition.lock();
            try {
                partition.flush();
            } finally {
                partition.unlock();
            }
        }
    }

    // Private

    private int getPartitionIndex(Object key) {
        int hash = key.hashCode();
        // make it positive
        if (hash < 0) {
            hash = hash == Integer.MIN_VALUE ? Integer.MAX_VALUE : -hash;
        }
        return hash % partitions.length;
    }

    // Inner

    private class Overager implements Runnable {
        private final long maxAgeMs;
        private final long periodMs;
        private boolean run = true;

        public Overager(long maxAgeMs, long periodMs) {
            this.maxAgeMs = maxAgeMs;
            this.periodMs = periodMs;
        }

        public void stop() {
            run = false;
        }

        public void run() {
            boolean intr = false;
            try {
                while (run) {
                    long lastUpdated = System.currentTimeMillis() - maxAgeMs;
                    for (int i = 0; i < partitions.length; ++i) {
                        partitions[i].ageOut(lastUpdated);
                    }

                    try {
                        Thread.sleep(periodMs);
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }
    }
}
