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

import org.jboss.as.ee.component.AbstractComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.strictmax.StrictMaxPool;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponent extends SessionBeanComponent implements PooledComponent<StatelessSessionComponentInstance> {
    // some more injectable resources
    // @Resource
    private Pool<StatelessSessionComponentInstance> pool;

    /**
     * Constructs a StatelessEJBComponent for a stateless session bean
     *
     * @param componentConfiguration
     */
    public StatelessSessionComponent(final StatelessSessionComponentConfiguration componentConfiguration) {
        super(componentConfiguration);

        StatelessObjectFactory<StatelessSessionComponentInstance> factory = new StatelessObjectFactory<StatelessSessionComponentInstance>() {
            @Override
            public StatelessSessionComponentInstance create() {
                return (StatelessSessionComponentInstance) createInstance();
            }

            @Override
            public void destroy(StatelessSessionComponentInstance obj) {
                destroyInstance(obj);
            }
        };
        this.pool = new StrictMaxPool<StatelessSessionComponentInstance>(factory, 20, 5, TimeUnit.MINUTES);
    }

    //TODO: This should be getInstance()
    @Override
    public ComponentInstance createInstance() {
        // TODO: Use a pool
        return super.createInstance();
    }

    @Override
    protected AbstractComponentInstance constructComponentInstance(Object instance, InterceptorFactoryContext context) {
        return new StatelessSessionComponentInstance(this, instance, context);
    }

    @Override
    public Pool<StatelessSessionComponentInstance> getPool() {
        return pool;
    }

    @Override
    public Object invoke(Serializable sessionId, Map<String, Object> contextData, Class<?> invokedBusinessInterface, Method beanMethod, Object[] args) throws Exception {
        if (sessionId != null)
            throw new IllegalArgumentException("Stateless " + this + " does not support sessions");
        if (invokedBusinessInterface != null)
            throw new UnsupportedOperationException("invokedBusinessInterface != null");
        InterceptorContext context = new InterceptorContext();
        context.putPrivateData(Component.class, this);
        context.setContextData(contextData);
        context.setMethod(beanMethod);
        context.setParameters(args);
        return getComponentInterceptor().processInvocation(context);
    }
}
