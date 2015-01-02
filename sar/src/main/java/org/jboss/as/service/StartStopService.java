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

package org.jboss.as.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for legacy JBoss services that controls the service start and stop lifecycle.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class StartStopService extends AbstractService {

    private final Method startMethod;
    private final Method stopMethod;

    StartStopService(final Object mBeanInstance, final Method startMethod, final Method stopMethod, final List<SetupAction> setupActions,  final ClassLoader mbeanContextClassLoader) {
        super(mBeanInstance, setupActions, mbeanContextClassLoader);
        this.startMethod = startMethod;
        this.stopMethod = stopMethod;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Starting Service: %s", context.getController().getName());
        }
        final Runnable task = new Runnable() {
            @Override
            public void run() {
            try {
                invokeLifecycleMethod(startMethod, context);
                context.complete();
            } catch (Throwable e) {
                context.failed(new StartException(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("start()"), e));
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
            SarLogger.ROOT_LOGGER.tracef("Stopping Service: %s", context.getController().getName());
        }
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    invokeLifecycleMethod(stopMethod, context);
                } catch (Exception e) {
                    SarLogger.ROOT_LOGGER.error(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("stop()"), e);
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
