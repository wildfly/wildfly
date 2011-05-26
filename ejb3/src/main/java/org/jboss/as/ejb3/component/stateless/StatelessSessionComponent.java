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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.strictmax.StrictMaxPool;
import org.jboss.invocation.Interceptor;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponent extends SessionBeanComponent implements PooledComponent<StatelessSessionComponentInstance> {

    private Pool<StatelessSessionComponentInstance> pool;

    /**
     * Constructs a StatelessEJBComponent for a stateless session bean
     *
     * @param ejbComponentCreateService
     */
    public StatelessSessionComponent(final SessionBeanComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

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
        this.pool = new StrictMaxPool<StatelessSessionComponentInstance>(factory, 20, 5, TimeUnit.MINUTES);
    }


//    @Override
//    public Interceptor createClientInterceptor(Class<?> viewClass) {
//        return new Interceptor() {
//            @Override
//            public Object processInvocation(InterceptorContext context) throws Exception {
//                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
//                // runs on remote clients.
//                context.putPrivateData(Component.class, StatelessSessionComponent.this);
//                try {
//                    final Method method = context.getMethod();
//                    if(isAsynchronous(method)) {
//                        return invokeAsynchronous(method, context);
//                    }
//                    return context.proceed();
//                }
//                finally {
//                    context.putPrivateData(Component.class, null);
//                }
//            }
//        };
//    }
//
//    @Override
//    public Interceptor createClientInterceptor(Class<?> view, Serializable sessionId) {
//        return createClientInterceptor(view);
//    }


    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors) {
        return new StatelessSessionComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);

    }

    @Override
    public Pool<StatelessSessionComponentInstance> getPool() {
        return pool;
    }
}
