/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.as.concurrent;

import org.wildfly.as.concurrent.context.Context;
import org.wildfly.as.concurrent.context.ContextConfiguration;

import javax.enterprise.concurrent.ManagedThreadFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryImpl implements ManagedThreadFactory {

    private final ContextConfiguration contextConfiguration;
    private final ReentrantLock lock;
    private final Set<ManageableThreadImpl> threads;
    private volatile boolean shutdown;

    public ManagedThreadFactoryImpl(ContextConfiguration contextConfiguration) {
        // TODO add thread groups, thread name prefix, priority, etc
        this.contextConfiguration = contextConfiguration;
        this.threads = new HashSet<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public Thread newThread(final Runnable r) {
        lock.lock();
        if (shutdown) {
            throw new IllegalStateException();
        }
        try {
            final ManageableThreadImpl t;
            if (System.getSecurityManager() == null) {
                t = newManageableThread(r);
            } else {
                t = AccessController.doPrivileged(
                        new PrivilegedAction<ManageableThreadImpl>() {
                            @Override
                            public ManageableThreadImpl run() {
                                return newManageableThread(r);
                            }
                        });
            }
            t.setDaemon(true);
            threads.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    ManageableThreadImpl newManageableThread(Runnable r) {
        final Context context = contextConfiguration != null ? contextConfiguration.newManageableThreadContext(r) : null;
        return new ManageableThreadImpl(r, context, this);
    }

    void remove(Thread thread) {
        lock.lock();
        try {
            threads.remove(thread);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return true if the factory is shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Initiates an orderly shutdown in which existent threads continue to run, but no new threads will be created.
     * Invocation has no additional effect if already shut down.
     */
    public void shutdown() {
        lock.lock();
        try {
            if (isShutdown()) {
                return;
            }
            // change state of the factory
            shutdown = true;
            // warn managedtask about shutdown
            for (ManageableThreadImpl thread : threads) {
                thread.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }

    List<? extends Thread> getThreads() {
        lock.lock();
        try {
            return new ArrayList<>(this.threads);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Besides the orderly shutdown, this method attempts to stop all existent threads, through interrupt calls.
     */
    public List<? extends Thread> shutdownNow() {
        lock.lock();
        try {
            if (isShutdown()) {
                return null;
            }
            // changes state and warns threads
            shutdown();
            // interrupt threads
            List<? extends Thread> result = getThreads();
            for (Thread thread : result) {
                try {
                    thread.interrupt();
                } catch (Throwable e) {
                    // ignore
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

}
