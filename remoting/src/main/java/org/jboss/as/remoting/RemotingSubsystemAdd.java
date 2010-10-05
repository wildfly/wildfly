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

package org.jboss.as.remoting;

import java.util.concurrent.Executor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.remoting3.Endpoint;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingSubsystemAdd extends AbstractSubsystemAdd<RemotingSubsystemElement> {

    private static final long serialVersionUID = -3368184946165491737L;

    private final String threadPoolName;

    protected RemotingSubsystemAdd(final String threadPoolName) {
        super(Namespace.CURRENT.getUriString());
        this.threadPoolName = threadPoolName;
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        // create endpoint
        final EndpointService endpointService = new EndpointService();
        final BatchServiceBuilder<Endpoint> endpointBuilder = updateContext.getBatchBuilder().addService(RemotingServices.ENDPOINT, endpointService);
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        endpointBuilder.addDependency(ThreadsServices.executorName(threadPoolName), new CastingInjector<Executor>(executorInjector, Executor.class));
        endpointBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);
    }

    protected RemotingSubsystemElement createSubsystemElement() {
        final RemotingSubsystemElement element = new RemotingSubsystemElement();
        element.setThreadPoolName(threadPoolName);
        return element;
    }
}
