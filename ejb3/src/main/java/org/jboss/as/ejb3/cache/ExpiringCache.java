/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import org.jboss.ejb.client.SessionID;
import org.jboss.tm.TxUtils;

/**
 * Cache that handles EJB expiration. This cache can be wrapped around an existing cache to
 * provide expiration functionality.
 *
 * @author Stuart Douglas
 */
public class ExpiringCache<T extends Identifiable> implements Cache<T> {

    private static final long SLEEP_TIME = 500;

    private final long millisecondTimeout;
    private final String beanName;
    private final Map<SessionID, Entry> cache;

    private volatile StatefulObjectFactory<T> factory;
    private volatile ExpirationTask expiryThread;

    private class ExpirationTask extends Thread {

        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {

                final List<Entry> queue = new ArrayList<Entry>();
                final long time = System.currentTimeMillis();
                synchronized (cache) {
                    Iterator<Map.Entry<SessionID, Entry>> iterator = cache.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Map.Entry<SessionID, Entry> entry = iterator.next();
                        if (entry.getValue().isExpired(time)) {
                            queue.add(entry.getValue());
                            iterator.remove();
                        }
                    }
                }
                for (Entry value : queue) {
                    try {
                        ROOT_LOGGER.debugf("Removing stateful bean %s - %s as it has been inactive for %d milliseconds", beanName, value.getKey(), millisecondTimeout);
                        factory.destroyInstance(value.getValue());
                    } catch (Exception e) {
                        ROOT_LOGGER.errorRemovingStatefulBean(value.getKey(), e);
                    }
                }

                try {
                    sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    running = false;
                    return;
                }
            }
        }

        public void stopTask() {
            running = false;
        }
    }

    private enum State {
        IN_USE, INACTIVE
    }

    private final class Entry {
        private long lastUsed;
        private State state = State.IN_USE;
        private final T value;

        public Entry(final T value) {
            this.value = value;
            this.lastUsed = System.currentTimeMillis();
        }

        public void setLastUsed(final long lastUsed) {
            this.lastUsed = lastUsed;
        }

        public SessionID getKey() {
            return value.getId();
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public boolean isExpired(long currentTimeInMillis) {
            return state == State.INACTIVE && currentTimeInMillis > lastUsed + millisecondTimeout;
        }

        public T getValue() {
            return value;
        }
    }

    public ExpiringCache(long value, TimeUnit timeUnit, final String beanName) {
        this.beanName = beanName;
        millisecondTimeout = TimeUnit.MILLISECONDS.convert(value, timeUnit);
        cache = new HashMap<SessionID, Entry>();
    }

    @Override
    public T create() {
        T obj = factory.createInstance();
        Entry entry = new Entry(obj);
        synchronized (cache) {
            cache.put(obj.getId(), entry);
        }
        return obj;
    }

    @Override
    public void discard(final SessionID key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    @Override
    public T get(final SessionID key) {
        synchronized (cache) {
            Entry val = cache.get(key);
            if (val == null) {
                return null;
            }
            val.lastUsed = System.currentTimeMillis();
            val.state = State.IN_USE;
            return val.getValue();
        }
    }

    @Override
    public void release(final T obj) {
        synchronized (cache) {
            Entry entry = cache.get(obj.getId());

            if (entry == null) {
                ROOT_LOGGER.couldNotFindStatefulBean(obj.getId());
                return;
            }
            //this must stay within the synchronized block so the changes are visible to other threads
            entry.lastUsed = System.currentTimeMillis();
            entry.state = State.INACTIVE;
        }
    }

    @Override
    public void remove(final TransactionManager transactionManager, final SessionID key) {

        final Transaction currentTx;
        try {
            currentTx = transactionManager.getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }


        final Entry object;
        synchronized (cache) {
            object = cache.remove(key);
        }

        if (currentTx != null && TxUtils.isActive(currentTx)) {
            try {
                // A transaction is in progress, so register a Synchronization so that the session can be removed on tx
                // completion.
                currentTx.registerSynchronization(new RemoveSynchronization(object));
            } catch (RollbackException e) {
                throw new RuntimeException(e);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        } else {
            // no tx currently in progress, so just remove the session
            factory.destroyInstance(object.value);
        }

    }

    @Override
    public void setStatefulObjectFactory(final StatefulObjectFactory<T> tStatefulObjectFactory) {
        this.factory = tStatefulObjectFactory;
    }

    @Override
    public synchronized void start() {
        if (millisecondTimeout >= 0) {
            expiryThread = new ExpirationTask();
            expiryThread.setDaemon(true);
            expiryThread.start();
            expiryThread.setName("Expiry Thread for SFSB " + beanName);
        }
    }

    @Override
    public synchronized void stop() {
        if (expiryThread != null) {
            expiryThread.stopTask();
        }
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * A {@link javax.transaction.Synchronization} which removes a stateful session in it's {@link javax.transaction.Synchronization#afterCompletion(int)}
     * callback.
     */
    private class RemoveSynchronization implements Synchronization {
        private final Entry object;

        public RemoveSynchronization(final Entry object) {
            this.object = object;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            factory.destroyInstance(object.value);
        }

    }
}
