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
package org.jboss.as.ejb3.component.session;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInjector;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.AsyncFutureInterceptor;
import org.jboss.as.ejb3.component.AsyncVoidInterceptor;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.AccessTimeout;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponent extends EJBComponent implements org.jboss.ejb3.context.spi.SessionBeanComponent {

    private static final Logger logger = Logger.getLogger(SessionBeanComponent.class);

    static final ServiceName ASYNC_EXECUTOR_SERVICE_NAME = ThreadsServices.EXECUTOR.append("ejb3-async");

    protected AccessTimeout beanLevelAccessTimeout;
    private final Set<Method> asynchronousMethods;
    protected Executor asyncExecutor;

    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     */
    protected SessionBeanComponent(final SessionBeanComponentConfiguration configuration) {
        super(configuration);

        AccessTimeout accessTimeout = configuration.getBeanLevelAccessTimeout();
        // TODO: the configuration should always have an access timeout
        if (accessTimeout == null) {
            accessTimeout = new AccessTimeout() {
                @Override
                public long value() {
                    return 5;
                }

                @Override
                public TimeUnit unit() {
                    return MINUTES;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return AccessTimeout.class;
                }
            };
        }
        this.beanLevelAccessTimeout = accessTimeout;
        this.asynchronousMethods = configuration.getAsynchronousMethods();
        this.asyncExecutor = (Executor) configuration.getInjection(ASYNC_EXECUTOR_SERVICE_NAME).getValue();
    }

    @Override
    protected List<ComponentInjector.InjectionHandle> applyInjections(ComponentInstance instance) {
        // TODO: a temporary hack until injection interceptors are in place
        BaseSessionInvocationContext invocationContext = new BaseSessionInvocationContext(null, null, null) {
            @Override
            public Object proceed() throws Exception {
                throw new RuntimeException("Do not call proceed");
            }
        };
        invocationContext.setEJBContext(((SessionBeanComponentInstance) instance).getSessionContext());
        CurrentInvocationContext.push(invocationContext);
        try {
            return super.applyInjections(instance);
        } finally {
            CurrentInvocationContext.pop();
        }
    }

    @Override
    public <T> T getBusinessObject(SessionContext ctx, Class<T> businessInterface) throws IllegalStateException {
        final ComponentView view = getComponentView(businessInterface);
        if (view == null)
            throw new IllegalStateException("Stateful bean " + getComponentName() + " does not have a view " + businessInterface);
        // see SessionBeanComponentInstance
        Serializable sessionId = ((SessionBeanComponentInstance.SessionBeanComponentInstanceContext) ctx).getId();
        Object proxy = view.getViewForInstance(sessionId);
        return businessInterface.cast(proxy);
    }

    @Override
    public EJBLocalObject getEJBLocalObject(SessionContext ctx) throws IllegalStateException {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.session.SessionBeanComponent.getEJBLocalObject");
    }

    @Override
    public EJBObject getEJBObject(SessionContext ctx) throws IllegalStateException {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.session.SessionBeanComponent.getEJBObject");
    }

    /**
     * Returns the {@link AccessTimeout} applicable to the bean
     *
     * @return
     */
    public AccessTimeout getAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    /**
     * Return the {@link Executor} used for asynchronous invocations.
     *
     * @return the async executor
     */
    public Executor getAsynchronousExecutor() {
        return asyncExecutor;
    }

    protected boolean isAsynchronous(final Method method) {
        final Set<Method> asyncMethods = this.asynchronousMethods;
        if (asyncMethods == null) {
            return false;
        }

        for (Method asyncMethod : asyncMethods) {
            if (method.getName().equals(asyncMethod.getName())) {
                final Object[] methodParams = method.getParameterTypes();
                final Object[] asyncMethodParams = asyncMethod.getParameterTypes();
                if (Arrays.equals(methodParams, asyncMethodParams)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract Object invoke(Serializable sessionId, Map<String, Object> contextData, Class<?> invokedBusinessInterface, Method implMethod, Object[] args) throws Exception;

    protected Object invokeAsynchronous(final Method method, final InterceptorContext context) throws Exception {
        if (Void.TYPE.isAssignableFrom(method.getReturnType())) {
            return new AsyncVoidInterceptor(getAsynchronousExecutor()).processInvocation(context);
        } else {
            return new AsyncFutureInterceptor(getAsynchronousExecutor()).processInvocation(context);
        }
    }

    @Override
    public Interceptor createClientInterceptor(Class<?> view, Serializable sessionId) {
        // ignore the session id. Session aware components like (StatefulSessionComponent) should override
        // this method to take into account the session id.
        return this.createClientInterceptor(view);
    }

    @Override
    public Interceptor createClientInterceptor(final Class<?> view) {

        return new Interceptor() {
            @Override
            public Object processInvocation(InterceptorContext context) throws Exception {
                final Method method = context.getMethod();
                // if no-interface view, then check whether invocation on the method is allowed
                // (for ex: invocation on protected methods isn't allowed)
                if (SessionBeanComponent.this.getComponentClass().equals(view)) {
                    if (!SessionBeanComponent.this.isInvocationAllowed(method)) {
                        throw new javax.ejb.EJBException("Cannot invoke method " + method
                                + " on nointerface view of bean " + SessionBeanComponent.this.getComponentName());

                    }
                }
                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
                // runs on remote clients.
                context.putPrivateData(Component.class, SessionBeanComponent.this);
                try {
                    if (isAsynchronous(method)) {
                        return invokeAsynchronous(method, context);
                    }
                    return context.proceed();
                } finally {
                    context.putPrivateData(Component.class, null);
                }
            }
        };
    }

    /**
     * EJB 3.1 spec mandates that the view should allow invocation on only public, non-final, non-static
     * methods. This method returns true if the passed {@link Method method} is public, non-static and non-final.
     * Else returns false.
     */
    protected boolean isInvocationAllowed(Method method) {
        int m = method.getModifiers();
        // We handle only public, non-static, non-final methods
        if (!Modifier.isPublic(m)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method " + method + " is *not* public");
            }
            // it's not a public method
            return false;
        }
        if (Modifier.isFinal(m)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method " + method + " is final");
            }
            // it's a final method
            return false;
        }
        if (Modifier.isStatic(m)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method " + method + " is static");
            }
            // it's a static method
            return false;
        }
        if (Modifier.isNative(m)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method " + method + " is native");
            }
            // it's a native method
            return false;
        }
        // we handle rest of the methods
        return true;
    }

}
