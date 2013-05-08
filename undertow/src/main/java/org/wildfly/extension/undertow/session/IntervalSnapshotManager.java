/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.session;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.wildfly.extension.undertow.UndertowLogger;

/**
 * A snapshot manager that collects all modified sessions over a given period of time and distributes them en bloc.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @author Brian Stansberry
 * @version $Revision: 89149 $
 */
public class IntervalSnapshotManager extends SnapshotManager implements Runnable {
    // the interval in ms
    private final int interval;

    // the modified sessions
    private final Set<ClusteredSession<? extends OutgoingDistributableSessionData>> sessions = new LinkedHashSet<ClusteredSession<? extends OutgoingDistributableSessionData>>();

    // the distribute thread
    private volatile Thread thread = null;

    // Is session processing allowed?
    private volatile boolean processingAllowed = false;

    // has the thread finished?
    private volatile boolean threadDone = false;

    private final ClassLoader classLoader;

    public IntervalSnapshotManager(SessionManager manager, final ClassLoader classLoader, String path) {
        super(manager,  path);
        this.classLoader = classLoader;
        this.interval =  1000;
    }

    public IntervalSnapshotManager(SessionManager manager,final ClassLoader classLoader, String path, int interval) {
        super(manager,  path);
        this.interval = interval;
        this.classLoader = classLoader;
    }

    /**
     * Store the modified session in a hashmap for the distributor thread
     */
    @Override
    public void snapshot(ClusteredSession<? extends OutgoingDistributableSessionData> session) {
        try {
            // Don't hold a ref to the session for a long time
            synchronized (sessions) {
                sessions.add(session);
            }
        } catch (Exception e) {
            UndertowLogger.ROOT_LOGGER.failedQueueingSessionReplication(session, e);
        }
    }

    /**
     * Distribute all modified sessions
     */
    protected void processSessions() {
        Set<ClusteredSession<? extends OutgoingDistributableSessionData>> toProcess = null;
        synchronized (sessions) {
            toProcess = new HashSet<ClusteredSession<? extends OutgoingDistributableSessionData>>(sessions);
            sessions.clear();
        }

        SessionManager mgr = getManager();
        for (ClusteredSession<? extends OutgoingDistributableSessionData> session : toProcess) {
            // Confirm we haven't been stopped
            if (!processingAllowed)
                break;

            try {
                mgr.storeSession(session);
            } catch (Exception e) {
                UndertowLogger.ROOT_LOGGER.failedToStoreSession(session.getRealId(), e);
            }
        }
    }

    /**
     * Start the snapshot manager
     */
    @Override
    public void start() {
        processingAllowed = true;
        startThread();
    }

    /**
     * Stop the snapshot manager
     */
    @Override
    public void stop() {
        processingAllowed = false;
        stopThread();
        synchronized (sessions) {
            sessions.clear();
        }
    }

    /**
     * Start the distributor thread
     */
    protected void startThread() {
        if (thread != null) {
            return;
        }

        thread = new Thread(this, "ClusteredSessionDistributor[" + getContextPath() + "]");
        thread.setDaemon(true);
        thread.setContextClassLoader(classLoader);
        threadDone = false;
        thread.start();
    }

    /**
     * Stop the distributor thread
     */
    protected void stopThread() {
        boolean intr = false;
        try {
            if (thread == null) {
                return;
            }
            threadDone = true;
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                intr = true;
            }
            thread = null;
        } finally {
            if (intr)
                Thread.currentThread().interrupt();
        }
    }

    /**
     * Thread-loop
     */
    @Override
    public void run() {
        boolean intr = false;
        try {
            while (!threadDone) {
                try {
                    Thread.sleep(interval);
                    processSessions();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    if (!threadDone)
                        UndertowLogger.ROOT_LOGGER.exceptionProcessingSessions(ie);
                } catch (Exception e) {
                    UndertowLogger.ROOT_LOGGER.exceptionProcessingSessions(e);
                }
            }
        } finally {
            if (intr)
                Thread.currentThread().interrupt();
        }
    }
}
