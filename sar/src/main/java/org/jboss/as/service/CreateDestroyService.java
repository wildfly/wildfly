/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private final Map<Method, Supplier<Object>> injections = new HashMap<>();
    private ManagedReference managedReference;

    CreateDestroyService(final Object mBeanInstance, final Method createMethod, final Method destroyMethod, ServiceComponentInstantiator componentInstantiator,
                         List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader, final Consumer<Object> mBeanInstanceConsumer) {
        super(mBeanInstance, setupActions, mbeanContextClassLoader, mBeanInstanceConsumer);
        this.createMethod = createMethod;
        this.destroyMethod = destroyMethod;
        this.componentInstantiator = componentInstantiator;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        super.start(context);
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Creating Service: %s", context.getController().provides());
        }
        try {
            injectDependencies();
            invokeLifecycleMethod(createMethod, context);
            if (componentInstantiator != null) {
                managedReference = componentInstantiator.initializeInstance(mBeanInstance);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            uninjectDependencies();
            throw new StartException(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("create()"), e);
        }
    }

    @Override
    public void stop(final StopContext context) {
        super.stop(context);
        if (SarLogger.ROOT_LOGGER.isTraceEnabled()) {
            SarLogger.ROOT_LOGGER.tracef("Destroying Service: %s", context.getController().provides());
        }
        try {
            if(managedReference != null) {
                managedReference.release();
            }
            invokeLifecycleMethod(destroyMethod, context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            SarLogger.ROOT_LOGGER.error(SarLogger.ROOT_LOGGER.failedExecutingLegacyMethod("destroy()"), e);
        } finally {
            uninjectDependencies();
        }
    }

    void inject(final Method setter, final Supplier<Object> injectionSupplier) {
        injections.put(setter, injectionSupplier);
    }

    private void injectDependencies() throws IllegalAccessException, InvocationTargetException {
        Method setter;
        Object arg;
        for (final Entry<Method, Supplier<Object>> injection : injections.entrySet()) {
            setter = injection.getKey();
            arg = injection.getValue().get();
            setter.invoke(mBeanInstance, arg);
        }
    }

    private void uninjectDependencies() {
        Method setter;
        for (final Entry<Method, Supplier<Object>> injection : injections.entrySet()) {
            try {
                setter = injection.getKey();
                setter.invoke(mBeanInstance, (Object[]) null);
            } catch (final Throwable ignored) {}
        }
    }

}
