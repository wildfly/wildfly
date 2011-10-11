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
package org.jboss.as.threads;

import org.jboss.threads.EventListener;
import org.jboss.threads.QueueExecutor;

/**
 *
 * @author Alexey Loubyansky
 */
public class ManagedQueueExecutorService extends ManagedExecutorService {

    private final QueueExecutor executor;

    public ManagedQueueExecutorService(QueueExecutor executor) {
        super(executor);
        this.executor = executor;
    }

    public int getCoreThreads() {
        return executor.getCoreThreads();
    }

    // Package protected for subsys write-attribute handlers
    void setCoreThreads(int coreThreads) {
        executor.setCoreThreads(coreThreads);
    }

    public boolean isAllowCoreTimeout() {
        return executor.isAllowCoreThreadTimeout();
    }

    void setAllowCoreTimeout(boolean allowCoreTimeout) {
        executor.setAllowCoreThreadTimeout(allowCoreTimeout);
    }

    public boolean isBlocking() {
        return executor.isBlocking();
    }

    void setBlocking(boolean blocking) {
        executor.setBlocking(blocking);
    }

    public int getMaxThreads() {
        return executor.getMaxThreads();
    }

    void setMaxThreads(int maxThreads) {
        executor.setMaxThreads(maxThreads);
    }

    public long getKeepAlive() {
        return executor.getKeepAliveTime();
    }

    void setKeepAlive(TimeSpec keepAlive) {
        executor.setKeepAliveTime(keepAlive.getDuration(), keepAlive.getUnit());
    }

    public int getCurrentThreadCount() {
        return executor.getCurrentThreadCount();
    }

    public int getLargestThreadCount() {
        return executor.getLargestThreadCount();
    }

    <A> void addShutdownListener(final EventListener<A> shutdownListener, final A attachment) {
        executor.addShutdownListener(shutdownListener, attachment);
    }
}
