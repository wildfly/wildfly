/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.service.component.ServiceComponentInstantiator;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for legacy JBoss services that controls the service create and destroy lifecycle.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
final class CreateDestroyService extends AbstractService {

    private final Method createMethod;
    private final Method destroyMethod;

    private final ServiceComponentInstantiator componentInstantiator;
    private ManagedReference managedReference;

    CreateDestroyService(final Object mBeanInstance, final Method createMethod, final Method destroyMethod, ServiceComponentInstantiator componentInstantiator,
                         List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader) {
        super(mBeanInstance, setupActions, mbeanContextClassLoader);
        this.createMethod = createMethod;
        this.destroyMethod = destroyMethod;
        this.componentInstantiator = componentInstantiator;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Creating Service: %s", context.getController().getName());
        }
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    invokeLifecycleMethod(createMethod, context);
                    if (componentInstantiator != null) {
                        managedReference = componentInstantiator.initializeInstance(getValue());
                    }
                    context.complete();
                } catch (Throwable e) {
                    context.failed(new StartException(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("create()"), e));
                }
            }
        };
        try {
            executor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Destroying Service: %s", context.getController().getName());
        }
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    if(managedReference != null) {
                        managedReference.release();
                    }
                    invokeLifecycleMethod(destroyMethod, context);
                } catch (Exception e) {
                    SarLogger.ROOT_LOGGER.error(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("destroy()"), e);
                } finally {
                    context.complete();
                }
            }
        };
        try {
            executor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

}
