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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.controller.NewModelController;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Installs the {@link NewAbstractModelControllerOperationHandlerService}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class NewAbstractModelControllerOperationHandlerService<T extends NewAbstractModelControllerOperationHandler> implements Service<T>{

    protected static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName OPERATION_HANDLER_NAME_SUFFIX = ServiceName.of("operation", "handler");

    private final InjectedValue<NewModelController> modelControllerValue = new InjectedValue<NewModelController>();

    private volatile ExecutorService executor = Executors.newCachedThreadPool();

    private volatile T handler;

    /**
     * Use to inject the model controller that will be the target of the operations
     *
     * @return the injected value holder
     */
    public InjectedValue<NewModelController> getModelControllerInjector() {
        return modelControllerValue;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting operation handler service %s", context.getController().getName());
        final NewModelController modelController = modelControllerValue.getValue();
        this.handler = createOperationHandler(modelController, executor);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        handler = null;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() throws IllegalStateException {
        return handler;
    }

    protected abstract T createOperationHandler(NewModelController modelController, ExecutorService executor);
}
