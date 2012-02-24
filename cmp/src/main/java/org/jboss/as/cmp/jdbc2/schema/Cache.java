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

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface Cache {
    class RemoveException extends RuntimeException {
        public RemoveException() {
        }

        public RemoveException(String message) {
            super(message);
        }

        public RemoveException(Throwable cause) {
            super(cause);
        }

        public RemoveException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    void lock();

    void lock(Object key);

    void unlock();

    void unlock(Object key);

    Object[] getFields(Object pk);

    Object[] getRelations(Object pk);

    void put(Transaction tx, Object pk, Object[] fields, Object[] relations);

    void remove(Transaction tx, Object pk) throws RemoveException;

    boolean contains(Transaction tx, Object pk);

    void lockForUpdate(Transaction tx, Object pk) throws Exception;

    void releaseLock(Transaction tx, Object pk) throws Exception;

    void flush();

    Cache NONE = new Cache() {
        public void lock() {
        }

        public void lock(Object key) {
        }

        public void unlock() {
        }

        public void unlock(Object key) {
        }

        public Object[] getFields(Object pk) {
            return null;
        }

        public Object[] getRelations(Object pk) {
            return null;
        }

        public void put(Transaction tx, Object pk, Object[] fields, Object[] relations) {
        }

        public void remove(Transaction tx, Object pk) {
        }

        public boolean contains(Transaction tx, Object pk) {
            return false;
        }

        public void lockForUpdate(Transaction tx, Object pk) throws Exception {
        }

        public void releaseLock(Transaction tx, Object pk) throws Exception {
        }

        public void flush() {
        }
    };

    interface CacheLoader {
        Object loadFromCache(Object value);

        Object getCachedValue();
    }

    interface Listener {
        void contention(int partitionIndex, long time);

        void eviction(int partitionIndex, Object pk, int size);

        void hit(int partitionIndex);

        void miss(int partitionIndex);

        Listener NOOP = new Listener() {
            public void contention(int partitionIndex, long time) {
            }

            public void eviction(int partitionIndex, Object pk, int size) {
            }

            public void hit(int partitionIndex) {
            }

            public void miss(int partitionIndex) {
            }
        };
    }
}
