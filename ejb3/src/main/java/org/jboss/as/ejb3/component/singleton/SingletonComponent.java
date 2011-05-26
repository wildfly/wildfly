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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.AccessTimeout;
import javax.ejb.LockType;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.StopContext;

/**
 * {@link Component} representing a {@link javax.ejb.Singleton} EJB.
 *
 * @author Jaikiran Pai
 */
public class SingletonComponent extends SessionBeanComponent implements LockableComponent {

    private static final Logger logger = Logger.getLogger(SingletonComponent.class);

    private SingletonComponentInstance singletonComponentInstance;

    private boolean initOnStartup;

    private LockType beanLevelLockType;

    private Map<EJBBusinessMethod, LockType> methodLockTypes;

    private Map<EJBBusinessMethod, AccessTimeout> methodAccessTimeouts;

    /**
     * Construct a new instance.
     *
     * @param singletonComponentCreateService
     *         the component configuration
     */
    public SingletonComponent(final SingletonComponentCreateService singletonComponentCreateService) {
        super(singletonComponentCreateService);
        this.initOnStartup = singletonComponentCreateService.isInitOnStartup();

        this.beanLevelLockType = singletonComponentCreateService.getBeanLockType();
        this.methodLockTypes = singletonComponentCreateService.getMethodApplicableLockTypes();
        this.methodAccessTimeouts = singletonComponentCreateService.getMethodApplicableAccessTimeouts();
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors) {
        if (this.singletonComponentInstance != null) {
            throw new IllegalStateException("A singleton component instance has already been created for bean: " + this.getComponentName());
        }
        this.singletonComponentInstance = new SingletonComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
        return this.singletonComponentInstance;
    }

    synchronized ComponentInstance getComponentInstance() {
        if (this.singletonComponentInstance == null) {
            this.singletonComponentInstance = (SingletonComponentInstance) this.createInstance();
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
        if (this.beanLevelLockType != null) {
            return this.beanLevelLockType;
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
        if (this.beanLevelAccessTimeout != null) {
            return this.beanLevelAccessTimeout;
        }
        return null;
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
            // TODO: Implement destroying an instance
            logger.warn("Destorying of singleton instance not yet implemented");
            //this.destroyInstance(this.singletonComponentInstance);
            this.singletonComponentInstance = null;
        }
    }
}
