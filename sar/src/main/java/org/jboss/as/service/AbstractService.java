/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Abstract service class.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractService implements Service<Object> {

    private final Object mBeanInstance;
    private final List<SetupAction> setupActions;
    private final ClassLoader mbeanContextClassLoader;
    protected final InjectedValue<ExecutorService> executor = new InjectedValue<ExecutorService>();


    /**
     * @param mBeanInstance
     * @param setupActions  actions to setup the thread local context
     */
    protected AbstractService(final Object mBeanInstance, final List<SetupAction> setupActions, final ClassLoader mbeanContextClassLoader) {
        this.mBeanInstance = mBeanInstance;
        this.setupActions = setupActions;
        this.mbeanContextClassLoader = mbeanContextClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getValue() {
        return mBeanInstance;
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

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executor;
    }

}
