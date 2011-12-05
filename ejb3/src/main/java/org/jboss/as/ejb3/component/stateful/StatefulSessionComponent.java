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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.AccessTimeout;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TimerService;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.ExpiringCache;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Stateful Session Bean
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponent extends SessionBeanComponent {

    public static final Object SESSION_ID_REFERENCE_KEY = new Object();


    private static final Logger logger = Logger.getLogger(StatefulSessionComponent.class);

    private final Cache<StatefulSessionComponentInstance> cache;

    private final InterceptorFactory afterBegin;
    private final Method afterBeginMethod;
    private final InterceptorFactory afterCompletion;
    private final Method afterCompletionMethod;
    private final InterceptorFactory beforeCompletion;
    private final Method beforeCompletionMethod;
    private final Map<EJBBusinessMethod, AccessTimeoutDetails> methodAccessTimeouts;
    private final DefaultAccessTimeoutService defaultAccessTimeoutProvider;


    private final InterceptorFactory ejb2XRemoveMethod;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected StatefulSessionComponent(final StatefulSessionComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        this.afterBegin = ejbComponentCreateService.getAfterBegin();
        this.afterBeginMethod = ejbComponentCreateService.getAfterBeginMethod();
        this.afterCompletion = ejbComponentCreateService.getAfterCompletion();
        this.afterCompletionMethod = ejbComponentCreateService.getAfterCompletionMethod();
        this.beforeCompletion = ejbComponentCreateService.getBeforeCompletion();
        this.beforeCompletionMethod = ejbComponentCreateService.getBeforeCompletionMethod();
        this.methodAccessTimeouts = ejbComponentCreateService.getMethodApplicableAccessTimeouts();
        this.defaultAccessTimeoutProvider = ejbComponentCreateService.getDefaultAccessTimeoutService();
        this.ejb2XRemoveMethod = ejbComponentCreateService.getEjb2XRemoveMethod();

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

    protected SessionID getSessionIdOf(final InterceptorContext ctx) {
        final StatefulSessionComponentInstance instance = (StatefulSessionComponentInstance) ctx.getPrivateData(ComponentInstance.class);
        return instance.getId();
    }

    public <T> T getBusinessObject(Class<T> businessInterface, final InterceptorContext context) throws IllegalStateException {
        if (businessInterface == null) {
            throw MESSAGES.businessInterfaceIsNull();
        }
        return createViewInstanceProxy(businessInterface, Collections.<Object, Object>singletonMap(SessionID.SESSION_ID_KEY, getSessionIdOf(context)));
    }

    public EJBLocalObject getEJBLocalObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbLocalObjectView() == null) {
            throw new IllegalStateException("Bean " + getComponentName() + " does not have an EJBLocalObject");
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.<Object, Object>singletonMap(SessionID.SESSION_ID_KEY, getSessionIdOf(ctx)), getEjbLocalObjectView());
    }

    public EJBObject getEJBObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbObjectView() == null) {
            throw MESSAGES.beanComponentMissingEjbObject(getComponentName(),"EJBObject");
        }
        final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getRequiredService(getEjbObjectView());
        final ComponentView view = (ComponentView) serviceController.getValue();
        final String locatorAppName = getEarApplicationName() == null ? "" : getEarApplicationName();
        return EJBClient.createProxy(new StatefulEJBLocator<EJBObject>((Class<EJBObject>) view.getViewClass(), locatorAppName, getModuleName(), getComponentName(), getDistinctName(), getSessionIdOf(ctx)));
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        throw MESSAGES.timerServiceNotSupportedForSFSB(this.getComponentName());
    }

    /**
     * Returns the {@link AccessTimeout} applicable to given method
     */
    public AccessTimeoutDetails getAccessTimeout(Method method) {
        final EJBBusinessMethod ejbMethod = new EJBBusinessMethod(method);
        final AccessTimeoutDetails accessTimeout = this.methodAccessTimeouts.get(ejbMethod);
        if (accessTimeout != null) {
            return accessTimeout;
        }
        // check bean level access timeout
        final AccessTimeoutDetails timeout = this.beanLevelAccessTimeout.get(method.getDeclaringClass().getName());
        if (timeout != null) {
            return timeout;
        }

        return defaultAccessTimeoutProvider.getDefaultAccessTimeout();
    }

    protected Interceptor createInterceptor(final InterceptorFactory factory) {
        if (factory == null)
            return null;
        final SimpleInterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(Component.class, this);
        return factory.create(context);
    }

    public SessionID createSession() {
        return getCache().create().getId();
    }

    public Cache<StatefulSessionComponentInstance> getCache() {
        return cache;
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {

        return new StatefulSessionComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors, ejb2XRemoveMethod.create(interceptorContext));
    }

    /**
     * Removes the session associated with the <code>sessionId</code>.
     *
     * @param sessionId The session id
     */
    public void removeSession(final SessionID sessionId) {
        //The cache takes care of the transactional behavoir
        getCache().remove(getTransactionManager(), sessionId);
    }

    public InterceptorFactory getAfterBegin() {
        return afterBegin;
    }

    public InterceptorFactory getAfterCompletion() {
        return afterCompletion;
    }

    public InterceptorFactory getBeforeCompletion() {
        return beforeCompletion;
    }

    public Method getAfterBeginMethod() {
        return afterBeginMethod;
    }

    public Method getAfterCompletionMethod() {
        return afterCompletionMethod;
    }

    public Method getBeforeCompletionMethod() {
        return beforeCompletionMethod;
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
