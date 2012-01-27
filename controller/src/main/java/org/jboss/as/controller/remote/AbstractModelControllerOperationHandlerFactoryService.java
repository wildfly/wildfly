/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.remote;

import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelController;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.controller.ControllerLogger.SERVER_MANAGEMENT_LOGGER;
import org.jboss.threads.JBossThreadFactory;

/**
 * Service used to create operation handlers per incoming channel
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractModelControllerOperationHandlerFactoryService implements Service<AbstractModelControllerOperationHandlerFactoryService>, ManagementChannelInitialization {

    public static final ServiceName OPERATION_HANDLER_NAME_SUFFIX = ServiceName.of("operation", "handler");

    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<ExecutorService> executor = new InjectedValue<ExecutorService>();

    /**
     * Use to inject the model controller that will be the target of the operations
     *
     * @return the injected value holder
     */
    public InjectedValue<ModelController> getModelControllerInjector() {
        return modelControllerValue;
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executor;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        SERVER_MANAGEMENT_LOGGER.debugf("Starting operation handler service %s", context.getController().getName());
        if(executor.getOptionalValue() == null) {
            final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("management-handler-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            final ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                                            5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                                            threadFactory);
            getExecutorInjector().inject(executorService);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        //
    }

    /** {@inheritDoc} */
    @Override
    public synchronized AbstractModelControllerOperationHandlerFactoryService getValue() throws IllegalStateException {
        return this;
    }

    protected ModelController getController() {
        return modelControllerValue.getValue();
    }

    protected ExecutorService getExecutor() {
        return executor.getValue();
    }

}
