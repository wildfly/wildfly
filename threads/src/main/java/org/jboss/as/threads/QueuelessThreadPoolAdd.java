/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.model.ChildElement;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class QueuelessThreadPoolAdd extends AbstractExecutorAdd {

    private static final long serialVersionUID = 5597662601486525937L;

    private String handoffExecutor;
    private boolean blocking;

    public QueuelessThreadPoolAdd(final String name, final ScaledCount maxThreads) {
        super(name, maxThreads);
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();
        final ScaledCount maxThreadsCount = getMaxThreads();
        final int maxThreads = maxThreadsCount.getScaledCount();
        final String name = getName();
        final QueuelessThreadPoolService service = new QueuelessThreadPoolService(maxThreads, blocking, getKeepaliveTime());
        builder.addService(ThreadsServices.executorName(name), service);
    }

    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
        final QueuelessThreadPoolElement poolElement = new QueuelessThreadPoolElement(getName());
        poolElement.setBlocking(blocking);
        poolElement.setHandoffExecutor(handoffExecutor);
        poolElement.setKeepaliveTime(getKeepaliveTime());
        poolElement.setThreadFactory(getThreadFactory());
        poolElement.setMaxThreads(getMaxThreads());
        element.addExecutor(getName(), new ChildElement<QueuelessThreadPoolElement>(Element.QUEUELESS_THREAD_POOL.getLocalName(), poolElement));
    }

    public String getHandoffExecutor() {
        return handoffExecutor;
    }

    public void setHandoffExecutor(final String handoffExecutor) {
        this.handoffExecutor = handoffExecutor;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(final boolean blocking) {
        this.blocking = blocking;
    }
}
