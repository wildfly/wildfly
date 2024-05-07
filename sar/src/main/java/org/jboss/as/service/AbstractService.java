/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Abstract service class.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractService implements Service {

    protected final Object mBeanInstance;
    private final List<SetupAction> setupActions;
    private final ClassLoader mbeanContextClassLoader;
    private final Consumer<Object> mBeanInstanceConsumer;

    /**
     * @param mBeanInstance
     * @param setupActions  actions to setup the thread local context
     */
    protected AbstractService(final Object mBeanInstance, final List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader, final Consumer<Object> mBeanInstanceConsumer) {
        this.mBeanInstance = mBeanInstance;
        this.setupActions = setupActions;
        this.mbeanContextClassLoader = mbeanContextClassLoader;
        this.mBeanInstanceConsumer = mBeanInstanceConsumer;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        mBeanInstanceConsumer.accept(mBeanInstance);
    }

    @Override
    public void stop(final StopContext context) {
        mBeanInstanceConsumer.accept(null);
    }

    protected void invokeLifecycleMethod(final Method method, final LifecycleContext context) throws InvocationTargetException, IllegalAccessException {
        if (method != null) {
            try {
                for (SetupAction action : setupActions) {
                    action.setup(Collections.<String, Object>emptyMap());
                }
                final ClassLoader old = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(this.mbeanContextClassLoader);
                try {
                    method.invoke(mBeanInstance);
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                }
            } finally {
                ListIterator<SetupAction> it = setupActions.listIterator(setupActions.size());
                while (it.hasPrevious()) {
                    SetupAction action = it.previous();
                    action.teardown(Collections.<String, Object>emptyMap());
                }
            }
        }
    }
}
