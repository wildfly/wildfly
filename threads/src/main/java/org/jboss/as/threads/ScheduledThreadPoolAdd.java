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

import java.util.concurrent.ExecutorService;

import org.jboss.as.model.ChildElement;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ScheduledThreadPoolAdd extends AbstractExecutorAdd {

    private static final long serialVersionUID = 5597662601486525937L;

    public ScheduledThreadPoolAdd(final String name, final ScaledCount maxThreads) {
        super(name, maxThreads);
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        final ServiceTarget target = updateContext.getServiceTarget();
        final ScaledCount maxThreadsCount = getMaxThreads();
        final int maxThreads = maxThreadsCount.getScaledCount();
        final String name = getName();
        final ServiceName serviceName = ThreadsServices.executorName(name);
        final UnboundedQueueThreadPoolService service = new UnboundedQueueThreadPoolService(maxThreads, getKeepaliveTime());
        final ServiceBuilder<ExecutorService> serviceBuilder = target.addService(serviceName, service);
        addThreadFactoryDependency(serviceName, serviceBuilder, service.getThreadFactoryInjector(), target);
        serviceBuilder.install();
    }

    @Override
    protected void applyUpdate(final ThreadsSubsystemElement element) throws UpdateFailedException {
        final ScheduledThreadPoolElement poolElement = new ScheduledThreadPoolElement(getName());
        poolElement.setKeepaliveTime(getKeepaliveTime());
        poolElement.setThreadFactory(getThreadFactory());
        poolElement.setMaxThreads(getMaxThreads());
        element.addExecutor(getName(), new ChildElement<ScheduledThreadPoolElement>(Element.SCHEDULED_THREAD_POOL.getLocalName(), poolElement));
    }
}
