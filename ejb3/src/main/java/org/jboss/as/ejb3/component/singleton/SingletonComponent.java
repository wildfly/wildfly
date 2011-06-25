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

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceRegistry;
import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StopContext;

import javax.ejb.AccessTimeout;
import javax.ejb.LockType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Component} representing a {@link javax.ejb.Singleton} EJB.
 *
 * @author Jaikiran Pai
 */
public class SingletonComponent extends SessionBeanComponent implements LockableComponent {

    private static final Logger logger = Logger.getLogger(SingletonComponent.class);

    private volatile SingletonComponentInstance singletonComponentInstance;

    private boolean initOnStartup;

    private Map<String, LockType> beanLevelLockType;

    private Map<EJBBusinessMethod, LockType> methodLockTypes;

    private Map<EJBBusinessMethod, AccessTimeout> methodAccessTimeouts;

    private final List<ServiceName> dependsOn;

    /**
     * Construct a new instance.
     *
     * @param singletonComponentCreateService
     *         the component configuration
     * @param dependsOn
     */
    public SingletonComponent(final SingletonComponentCreateService singletonComponentCreateService, final List<ServiceName> dependsOn) {
        super(singletonComponentCreateService);
        this.dependsOn = dependsOn;
        this.initOnStartup = singletonComponentCreateService.isInitOnStartup();

        this.beanLevelLockType = singletonComponentCreateService.getBeanLockType();
        this.methodLockTypes = singletonComponentCreateService.getMethodApplicableLockTypes();
        this.methodAccessTimeouts = singletonComponentCreateService.getMethodApplicableAccessTimeouts();
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors) {
        // synchronized from getComponentInstance
        assert Thread.holdsLock(this);
        // the race is on right through createInstance where everybody will wait until the component is started
        if (this.singletonComponentInstance != null) {
            return this.singletonComponentInstance;
        }
        if (dependsOn != null) {
            for(ServiceName serviceName : dependsOn) {
                final ServiceController<Component> service = (ServiceController<Component>) CurrentServiceRegistry.getServiceRegistry().getRequiredService(serviceName);
                final Component component = service.getValue();
                if(component instanceof SingletonComponent) {
                    ((SingletonComponent) component).getComponentInstance();
                }
            }
        }
        return new SingletonComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    ComponentInstance getComponentInstance() {
        if (this.singletonComponentInstance == null) {
            synchronized (this) {
                // no need to check here, createInstance has a wait (on start)
                this.singletonComponentInstance = (SingletonComponentInstance) this.createInstance();
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
            logger.debug(this.getComponentName() + " bean is a @Startup (a.k.a init-on-startup) bean, creating/getting the singleton instance");
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
    public AccessTimeout getAccessTimeout(Method method) {
        final EJBBusinessMethod ejbMethod = new EJBBusinessMethod(method);
        final AccessTimeout accessTimeout = this.methodAccessTimeouts.get(ejbMethod);
        if (accessTimeout != null) {
            return accessTimeout;
        }
        // check bean level access timeout
        final AccessTimeout beanTimeout = this.beanLevelAccessTimeout.get(method.getDeclaringClass().getName());
        if (beanTimeout != null) {
            return beanTimeout;
        }
        //TODO: this should be configurable
        return new AccessTimeout() {
            @Override
            public long value() {
                return 5;
            }

            @Override
            public TimeUnit unit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AccessTimeout.class;
            }
        };
    }

    @Override
    public AccessTimeout getDefaultAccessTimeout() {
        // TODO: This has to be configurable.
        // Currently defaults to 5 minutes
        return new AccessTimeout() {
            @Override
            public long value() {
                return 5;
            }

            @Override
            public TimeUnit unit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AccessTimeout.class;
            }
        };
    }

    private synchronized void destroySingletonInstance() {
        if (this.singletonComponentInstance != null) {
            singletonComponentInstance.destroy();
            this.singletonComponentInstance = null;
        }
    }
}
