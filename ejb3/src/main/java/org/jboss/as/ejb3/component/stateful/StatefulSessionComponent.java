/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.NoPassivationCache;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stateful Session Bean
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponent extends SessionBeanComponent {

    public static final Object SESSION_ATTACH_KEY = new Object();

    private Cache<StatefulSessionComponentInstance> cache;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected StatefulSessionComponent(final EJBComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);


        cache = new NoPassivationCache<StatefulSessionComponentInstance>();
        cache.setStatefulObjectFactory(new StatefulObjectFactory<StatefulSessionComponentInstance>() {
            @Override
            public StatefulSessionComponentInstance createInstance() {
                return (StatefulSessionComponentInstance) StatefulSessionComponent.this.createInstance();
            }

            @Override
            public void destroyInstance(StatefulSessionComponentInstance instance) {
                instance.destroy();
            }
        });
    }


//    @Override
//    public Interceptor createClientInterceptor(Class<?> view) {
//        final Serializable sessionId = createSession();
//        return createClientInterceptor(view, sessionId);
//    }
//
//    @Override
//    public Interceptor createClientInterceptor(final Class<?> view, final Serializable sessionId) {
//        return new Interceptor() {
//            @Override
//            public Object processInvocation(InterceptorContext context) throws Exception {
//                final Method method = context.getMethod();
//                // if no-interface view, then check whether invocation on the method is allowed
//                // (for ex: invocation on protected methods isn't allowed)
//                if (StatefulSessionComponent.this.getComponentClass().equals(view)) {
//                    if (!StatefulSessionComponent.this.isInvocationAllowed(method)) {
//                        throw new javax.ejb.EJBException("Cannot invoke method " + method
//                                + " on nointerface view of bean " + StatefulSessionComponent.this.getComponentName());
//
//                    }
//                }
//                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
//                // runs on remote clients.
//                context.putPrivateData(Component.class, StatefulSessionComponent.this);
//                // TODO: attaching as Serializable.class is a bit wicked
//                context.putPrivateData(Serializable.class, sessionId);
//                try {
//                    if (isAsynchronous(method)) {
//                        return invokeAsynchronous(method, context);
//                    }
//                    return context.proceed();
//                } finally {
//                    context.putPrivateData(Component.class, null);
//                    context.putPrivateData(Serializable.class, null);
//                }
//            }
//        };
//    }

    public Serializable createSession() {
        return getCache().create().getId();
    }

    public Cache<StatefulSessionComponentInstance> getCache() {
        return cache;
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors) {
        return new StatefulSessionComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }



    @Override
    public Object invoke(Serializable sessionId, Map<String, Object> contextData, Class<?> invokedBusinessInterface, Method beanMethod, Object[] args) throws Exception {
        if (sessionId == null)
            throw new IllegalArgumentException("Session is mandatory on Stateful " + this);
        if (invokedBusinessInterface != null)
            throw new UnsupportedOperationException("invokedBusinessInterface != null");
        InterceptorContext context = new InterceptorContext();
        context.putPrivateData(Component.class, this);
        // TODO: attaching as Serializable.class is a bit wicked
        context.putPrivateData(Serializable.class, sessionId);
        context.setContextData(contextData);
        context.setMethod(beanMethod);
        context.setParameters(args);
        throw new RuntimeException("NYI");
        //return getComponentInterceptor().processInvocation(context);
    }

}
