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

package org.jboss.as.ejb3.component.singleton;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.LockType;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.concurrency.LockableComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;

/**
 * {@link Component} representing a {@link javax.ejb.Singleton} EJB.
 *
 * @author Jaikiran Pai
 */
public class SingletonComponent extends SessionBeanComponent implements LockableComponent {

    private volatile SingletonComponentInstance singletonComponentInstance;

    private final boolean initOnStartup;

    private final Map<String, LockType> beanLevelLockType;

    private final Map<EJBBusinessMethod, LockType> methodLockTypes;

    private final Map<EJBBusinessMethod, AccessTimeoutDetails> methodAccessTimeouts;

    private final List<ServiceName> dependsOn;

    private final DefaultAccessTimeoutService defaultAccessTimeoutProvider;

    /**
     * We can't lock on <code>this</code> because the {@link org.jboss.as.ee.component.BasicComponent#waitForComponentStart()}
     * also synchronizes on it, and calls {@link #wait()}.
     */
    private final Object creationLock = new Object();

    /**
     * Construct a new instance.
     *
     * @param singletonComponentCreateService
     *                  the component configuration
     * @param dependsOn
     */
    public SingletonComponent(final SingletonComponentCreateService singletonComponentCreateService, final List<ServiceName> dependsOn) {
        super(singletonComponentCreateService);
        this.dependsOn = dependsOn;
        this.initOnStartup = singletonComponentCreateService.isInitOnStartup();

        this.beanLevelLockType = singletonComponentCreateService.getBeanLockType();
        this.methodLockTypes = singletonComponentCreateService.getMethodApplicableLockTypes();
        this.methodAccessTimeouts = singletonComponentCreateService.getMethodApplicableAccessTimeouts();
        this.defaultAccessTimeoutProvider = singletonComponentCreateService.getDefaultAccessTimeoutService();
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        // synchronized from getComponentInstance
        assert Thread.holdsLock(creationLock);

        if (dependsOn != null) {
            for (ServiceName serviceName : dependsOn) {
                final ServiceController<Component> service = (ServiceController<Component>) CurrentServiceContainer.getServiceContainer().getRequiredService(serviceName);
                final Component component = service.getValue();
                if (component instanceof SingletonComponent) {
                    ((SingletonComponent) component).getComponentInstance();
                }
            }
        }
        return new SingletonComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    public SingletonComponentInstance getComponentInstance() {
        if (this.singletonComponentInstance == null) {
            synchronized (creationLock) {
                if (this.singletonComponentInstance == null) {
                    this.singletonComponentInstance = (SingletonComponentInstance) this.createInstance();
                }
            }
        }
        return this.singletonComponentInstance;
    }

    @Override
    public void start() {
        super.start();
        if (this.initOnStartup) {
            // Do not call createInstance() because we can't ever assume that the singleton instance
            // hasn't already been created.
            ROOT_LOGGER.debug(this.getComponentName() + " bean is a @Startup (a.k.a init-on-startup) bean, creating/getting the singleton instance");
            this.getComponentInstance();
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        this.destroySingletonInstance();
        super.stop(stopContext);
    }

    @Override
    public LockType getLockType(Method method) {
        final EJBBusinessMethod ejbMethod = new EJBBusinessMethod(method);
        final LockType lockType = this.methodLockTypes.get(ejbMethod);
        if (lockType != null) {
            return lockType;
        }
        // check bean level lock type
        final LockType type = this.beanLevelLockType.get(method.getDeclaringClass().getName());
        if (type != null) {
            return type;
        }
        // default WRITE lock type
        return LockType.WRITE;
    }

    @Override
    public AccessTimeoutDetails getAccessTimeout(Method method) {
        final EJBBusinessMethod ejbMethod = new EJBBusinessMethod(method);
        final AccessTimeoutDetails accessTimeout = this.methodAccessTimeouts.get(ejbMethod);
        if (accessTimeout != null) {
            return accessTimeout;
        }
        // check bean level access timeout
        final AccessTimeoutDetails beanTimeout = this.beanLevelAccessTimeout.get(method.getDeclaringClass().getName());
        if (beanTimeout != null) {
            return beanTimeout;
        }
        return getDefaultAccessTimeout();
    }

    @Override
    public AccessTimeoutDetails getDefaultAccessTimeout() {
        return defaultAccessTimeoutProvider.getDefaultAccessTimeout();
    }

    private void destroySingletonInstance() {
        synchronized (creationLock) {
            if (this.singletonComponentInstance != null) {
                singletonComponentInstance.destroy();
                this.singletonComponentInstance = null;
            }
        }
    }

    @Override
    public AllowedMethodsInformation getAllowedMethodsInformation() {
        return SingletonAllowedMethodsInformation.INSTANCE;
    }
}
