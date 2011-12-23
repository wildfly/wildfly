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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponent extends SessionBeanComponent implements PooledComponent<StatelessSessionComponentInstance> {

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
            ROOT_LOGGER.debug("Pooling is disabled for Stateless EJB " + slsbComponentCreateService.getComponentName());
            this.pool = null;
        } else {
            ROOT_LOGGER.debug("Using pool config " + poolConfig + " to create pool for Stateless EJB " + slsbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
        }

        this.timeoutMethod = slsbComponentCreateService.getTimeoutMethod();
        final String deploymentName;
        if (slsbComponentCreateService.getDistinctName() == null || slsbComponentCreateService.getDistinctName().length() == 0) {
            deploymentName = slsbComponentCreateService.getApplicationName() + "." + slsbComponentCreateService.getModuleName();
        } else {
            deploymentName = slsbComponentCreateService.getApplicationName() + "." + slsbComponentCreateService.getModuleName() + "." + slsbComponentCreateService.getDistinctName();
        }
    }


    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        return new StatelessSessionComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    public Pool<StatelessSessionComponentInstance> getPool() {
        return pool;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
    }

    @Override
    public void start() {
        super.start();
        if(this.pool!=null){
            this.pool.start();
        }
    }


    @Override
    public void stop(StopContext stopContext) {
        if(this.pool!=null){
            this.pool.stop();
        }
        super.stop(stopContext);
    }

    @Override
    public AllowedMethodsInformation getAllowedMethodsInformation() {
        return StatelessAllowedMethodsInformation.INSTANCE;
    }
}
