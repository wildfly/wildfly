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

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.strictmax.StrictMaxPool;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponent extends SessionBeanComponent implements PooledComponent<StatelessSessionComponentInstance> {

    private static final Logger logger = Logger.getLogger(StatelessSessionComponent.class);

    private final Pool<StatelessSessionComponentInstance> pool;
    private final Method timeoutMethod;

    /**
     * Constructs a StatelessEJBComponent for a stateless session bean
     *
     * @param slsbComponentCreateService
     */
    public StatelessSessionComponent(final StatelessSessionComponentCreateService slsbComponentCreateService) {
        super(slsbComponentCreateService);

        StatelessObjectFactory<StatelessSessionComponentInstance> factory = new StatelessObjectFactory<StatelessSessionComponentInstance>() {
            @Override
            public StatelessSessionComponentInstance create() {
                return (StatelessSessionComponentInstance) createInstance();
            }

            @Override
            public void destroy(StatelessSessionComponentInstance obj) {
                obj.destroy();
            }
        };
        final PoolConfig poolConfig = slsbComponentCreateService.getPoolConfig();
        if (poolConfig == null) {
            logger.debug("Pooling is disabled for Stateless EJB " + slsbComponentCreateService.getComponentName());
            this.pool = null;
        } else {
            logger.debug("Using pool config " + poolConfig + " to create pool for Stateless EJB " + slsbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
        }

        this.timeoutMethod = slsbComponentCreateService.getTimeoutMethod();
    }


    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {

        final Map<Method, Interceptor> timeouts;
        if (timeoutInterceptors != null) {
            timeouts = new IdentityHashMap<Method, Interceptor>();
            for (Map.Entry<Method, InterceptorFactory> entry : timeoutInterceptors.entrySet()) {
                timeouts.put(entry.getKey(), entry.getValue().create(interceptorContext));
            }
        } else {
            timeouts = Collections.emptyMap();
        }
        return new StatelessSessionComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors, timeouts);
    }

    @Override
    public Pool<StatelessSessionComponentInstance> getPool() {
        return pool;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
    }
}
