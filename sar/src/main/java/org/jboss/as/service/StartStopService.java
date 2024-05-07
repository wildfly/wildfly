/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

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

    StartStopService(final Object mBeanInstance, final Method startMethod, final Method stopMethod, final List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader, final Consumer<Object> mBeanInstanceConsumer) {
        super(mBeanInstance, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer);
        this.startMethod = startMethod;
        this.stopMethod = stopMethod;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        super.start(context);
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Starting Service: %s", context.getController().provides());
        }
        try {
            invokeLifecycleMethod(startMethod, context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new StartException(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("start()"), e);
        }
    }

    @Override
    public void stop(final StopContext context) {
        super.stop(context);
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Stopping Service: %s", context.getController().provides());
        }
        try {
            invokeLifecycleMethod(stopMethod, context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            SarLogger.ROOT_LOGGER.error(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("stop()"), e);
        }
    }

}
