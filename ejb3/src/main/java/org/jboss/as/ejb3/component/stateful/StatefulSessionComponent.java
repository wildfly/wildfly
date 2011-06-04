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
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.StatefulObjectFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.TxUtils;

import javax.ejb.AccessTimeout;
import javax.ejb.TimerService;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Stateful Session Bean
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponent extends SessionBeanComponent {

    public static final Object SESSION_ATTACH_KEY = new Object();

    private Cache<StatefulSessionComponentInstance> cache;

    private static final Logger logger = Logger.getLogger(StatefulSessionComponent.class);

    final InterceptorFactory afterBegin;
    final InterceptorFactory afterCompletion;
    final InterceptorFactory beforeCompletion;
    private Map<EJBBusinessMethod, AccessTimeout> methodAccessTimeouts;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected StatefulSessionComponent(final StatefulSessionComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        this.afterBegin = ejbComponentCreateService.getAfterBegin();
        this.afterCompletion = ejbComponentCreateService.getAfterCompletion();
        this.beforeCompletion = ejbComponentCreateService.getBeforeCompletion();
        this.methodAccessTimeouts = ejbComponentCreateService.getMethodApplicableAccessTimeouts();

        final StatefulTimeoutInfo statefulTimeout = ejbComponentCreateService.getStatefulTimeout();
        if (statefulTimeout != null) {
            cache = new ExpiringCache<StatefulSessionComponentInstance>(statefulTimeout.getValue(), statefulTimeout.getTimeUnit(), ejbComponentCreateService.getComponentClass().getName());
        } else {
            cache = new ExpiringCache<StatefulSessionComponentInstance>(-1, TimeUnit.MILLISECONDS, ejbComponentCreateService.getComponentClass().getName());
        }
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

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        throw new IllegalStateException("TimerService is not supported for Stateful session bean " + this.getComponentName());
    }

    /**
     * Returns the {@link AccessTimeout} applicable to given method
     */
    public AccessTimeout getAccessTimeout(Method method) {
        final EJBBusinessMethod ejbMethod = new EJBBusinessMethod(method);
        final AccessTimeout accessTimeout = this.methodAccessTimeouts.get(ejbMethod);
        if (accessTimeout != null) {
            return accessTimeout;
        }
        // check bean level access timeout
        final AccessTimeout timeout = this.beanLevelAccessTimeout.get(method.getDeclaringClass().getName());
        if (timeout != null) {
            return timeout;
        }
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

    protected Interceptor createInterceptor(final InterceptorFactory factory) {
        if (factory == null)
            return null;
        final SimpleInterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(Component.class, this);
        return factory.create(context);
    }

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

    /**
     * Removes the session associated with the <code>sessionId</code>.
     *
     * @param sessionId The session id
     */
    public void removeSession(final Serializable sessionId) {
        Transaction currentTx = null;
        try {
            currentTx = getTransactionManager().getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }

        if (currentTx != null && TxUtils.isActive(currentTx)) {
            try {
                // A transaction is in progress, so register a Synchronization so that the session can be removed on tx
                // completion.
                currentTx.registerSynchronization(new RemoveSynchronization(this, sessionId));
            } catch (RollbackException e) {
                throw new RuntimeException(e);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        } else {
            // no tx currently in progress, so just remove the session
            getCache().remove(sessionId);
        }

    }

    /**
     * A {@link javax.transaction.Synchronization} which removes a stateful session in it's {@link javax.transaction.Synchronization#afterCompletion(int)}
     * callback.
     */
    private static class RemoveSynchronization implements Synchronization {
        private final StatefulSessionComponent statefulComponent;
        private final Serializable sessionId;

        public RemoveSynchronization(final StatefulSessionComponent component, final Serializable sessionId) {
            if (sessionId == null) {
                throw new IllegalArgumentException("Session id cannot be null");
            }
            if (component == null) {
                throw new IllegalArgumentException("Stateful component cannot be null");
            }
            this.sessionId = sessionId;
            this.statefulComponent = component;

        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            try {
                // remove the session
                this.statefulComponent.getCache().remove(this.sessionId);
            } catch (Throwable t) {
                // An exception thrown from afterCompletion is gobbled up
                logger.error("Failed to remove bean: " + this.statefulComponent.getComponentName() + " with session id " + this.sessionId, t);
                if (t instanceof Error)
                    throw (Error) t;
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                throw new RuntimeException(t);
            }
        }
    }

    @Override
    public void start() {
        super.start();
        cache.start();
    }

    @Override
    public void stop(final StopContext stopContext) {
        super.stop(stopContext);
        cache.stop();
    }
}
