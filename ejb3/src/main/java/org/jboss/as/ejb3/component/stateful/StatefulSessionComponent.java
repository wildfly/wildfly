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
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentIsStoppedException;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Stateful Session Bean
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponent extends SessionBeanComponent implements StatefulObjectFactory<StatefulSessionComponentInstance>, PassivationListener<StatefulSessionComponentInstance>, IdentifierFactory<SessionID> {

    private volatile Cache<SessionID, StatefulSessionComponentInstance> cache;

    private final InterceptorFactory afterBegin;
    private Interceptor afterBeginInterceptor;
    private final Method afterBeginMethod;
    private final InterceptorFactory afterCompletion;
    private Interceptor afterCompletionInterceptor;
    private final Method afterCompletionMethod;
    private final InterceptorFactory beforeCompletion;
    private Interceptor beforeCompletionInterceptor;
    private final Method beforeCompletionMethod;
    private final InterceptorFactory prePassivate;
    private Interceptor prePassivateInterceptor;
    private final InterceptorFactory postActivate;
    private Interceptor postActivateInterceptor;
    private final Map<EJBBusinessMethod, AccessTimeoutDetails> methodAccessTimeouts;
    private final DefaultAccessTimeoutService defaultAccessTimeoutProvider;
    private final Value<CacheFactory> cacheFactory;
    private final InterceptorFactory ejb2XRemoveMethod;
    private Interceptor ejb2XRemoveMethodInterceptor;

    /**
     * Set of context keys for serializable interceptors.
     * <p/>
     * These are used to serialize the user provided interceptors
     */
    private final Set<Object> serialiableInterceptorContextKeys;

    private final TimerService timerService;

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
        this.prePassivate = ejbComponentCreateService.getPrePassivate();
        this.postActivate = ejbComponentCreateService.getPostActivate();
        this.methodAccessTimeouts = ejbComponentCreateService.getMethodApplicableAccessTimeouts();
        this.defaultAccessTimeoutProvider = ejbComponentCreateService.getDefaultAccessTimeoutService();
        this.ejb2XRemoveMethod = ejbComponentCreateService.getEjb2XRemoveMethod();
        this.serialiableInterceptorContextKeys = ejbComponentCreateService.getSerializableInterceptorContextKeys();
        this.timerService = ejbComponentCreateService.getTimerService();
        this.cacheFactory = ejbComponentCreateService.getCacheFactory();
    }

    @Override
    public StatefulSessionComponentInstance createInstance() {
        return (StatefulSessionComponentInstance) super.createInstance();
    }

    @Override
    public StatefulSessionComponentInstance createInstance(final Object instance) {
        return (StatefulSessionComponentInstance) super.createInstance();
    }

    @Override
    protected StatefulSessionComponentInstance constructComponentInstance(ManagedReference instance, boolean invokePostConstruct) {
        return (StatefulSessionComponentInstance) super.constructComponentInstance(instance, invokePostConstruct);
    }

    @Override
    protected StatefulSessionComponentInstance constructComponentInstance(ManagedReference instance, boolean invokePostConstruct, Map<Object, Object> context) {
        return (StatefulSessionComponentInstance) super.constructComponentInstance(instance, invokePostConstruct, context);
    }

    @Override
    public void destroyInstance(StatefulSessionComponentInstance instance) {
        instance.setRemoved(true);
        if(!instance.isSynchronizationRegistered()) {
            instance.destroy();
        }
    }

    @Override
    public void postActivate(StatefulSessionComponentInstance instance) {
        instance.postActivate();
    }

    @Override
    public void prePassivate(StatefulSessionComponentInstance instance) {
        instance.prePassivate();
    }

    protected SessionID getSessionIdOf(final InterceptorContext ctx) {
        final StatefulSessionComponentInstance instance = (StatefulSessionComponentInstance) ctx.getPrivateData(ComponentInstance.class);
        return instance.getId();
    }

    @Override
    public <T> T getBusinessObject(Class<T> businessInterface, final InterceptorContext context) throws IllegalStateException {
        if (businessInterface == null) {
            throw EjbLogger.ROOT_LOGGER.businessInterfaceIsNull();
        }
        return createViewInstanceProxy(businessInterface, Collections.<Object, Object>singletonMap(SessionID.class, getSessionIdOf(context)));
    }

    @Override
    public EJBLocalObject getEJBLocalObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbLocalObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.ejbLocalObjectUnavailable(getComponentName());
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.<Object, Object>singletonMap(SessionID.class, getSessionIdOf(ctx)), getEjbLocalObjectViewServiceName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public EJBObject getEJBObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.beanComponentMissingEjbObject(getComponentName(), "EJBObject");
        }
        final ServiceController<?> serviceController = currentServiceContainer().getRequiredService(getEjbObjectViewServiceName());
        final ComponentView view = (ComponentView) serviceController.getValue();
        final String locatorAppName = getEarApplicationName() == null ? "" : getEarApplicationName();
        if(WildFlySecurityManager.isChecking()) {
            //need to use doPrivileged rather than doUnchecked, as this can run user code in the proxy constructor
            return AccessController.doPrivileged(new PrivilegedAction<EJBObject>() {
                @Override
                public EJBObject run() {
                   return EJBClient.createProxy(new StatefulEJBLocator<EJBObject>((Class<EJBObject>) view.getViewClass(), locatorAppName, getModuleName(), getComponentName(), getDistinctName(), getSessionIdOf(ctx), getCache().getStrictAffinity()));
                }
            });
        } else {
            return EJBClient.createProxy(new StatefulEJBLocator<EJBObject>((Class<EJBObject>) view.getViewClass(), locatorAppName, getModuleName(), getComponentName(), getDistinctName(), getSessionIdOf(ctx), this.getCache().getStrictAffinity()));
        }
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        return this.timerService;
    }

    /**
     * Returns the {@link javax.ejb.AccessTimeout} applicable to given method
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

    public SessionID createSession() {
        return this.cache.create().getId();
    }

    /**
     * creates a session using the global request controller.
     *
     * This should only be used by callers that service remote requests (i.e. places that represent a remote entry point into the container)
     * @return The session id
     */
    public SessionID createSessionRemote() {
        ControlPoint controlPoint = getControlPoint();
        if(controlPoint == null) {
            return createSession();
        } else {
            try {
                RunResult result = controlPoint.beginRequest();
                if (result == RunResult.REJECTED) {
                    throw EjbLogger.ROOT_LOGGER.containerSuspended();
                }
                try {
                    return createSession();
                } finally {
                    controlPoint.requestComplete();
                }
            } catch (EJBComponentUnavailableException | ComponentIsStoppedException e) {
                throw e;
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
    }
    public Cache<SessionID, StatefulSessionComponentInstance> getCache() {
        return this.cache;
    }

    @Override
    public SessionID createIdentifier() {
        return new UUIDSessionID(UUID.randomUUID());
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, Map<Object, Object> context) {
        StatefulSessionComponentInstance instance = new StatefulSessionComponentInstance(this, preDestroyInterceptor, methodInterceptors, context);
        for(Object key : serialiableInterceptorContextKeys) {
            instance.setInstanceData(key, context.get(key));
        }
        instance.setInstanceData(BasicComponentInstance.INSTANCE_KEY, context.get(BasicComponentInstance.INSTANCE_KEY));
        return instance;
    }

    /**
     * Removes the session associated with the <code>sessionId</code>.
     *
     * @param sessionId The session id
     */
    public void removeSession(final SessionID sessionId) {
        //The cache takes care of the transactional behavoir
        this.cache.remove(sessionId);
    }

    public Interceptor getAfterBegin() {
        return afterBeginInterceptor;
    }

    public Interceptor getAfterCompletion() {
        return afterCompletionInterceptor;
    }

    public Interceptor getBeforeCompletion() {
        return beforeCompletionInterceptor;
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

    public Interceptor getPrePassivate() {
        return this.prePassivateInterceptor;
    }

    public Interceptor getPostActivate() {
        return this.postActivateInterceptor;
    }

    public Interceptor getEjb2XRemoveMethod() {
        return this.ejb2XRemoveMethodInterceptor;
    }

    @Override
    public void init() {
        super.init();

        this.cache = this.cacheFactory.getValue().createCache(this, this, this);
        this.cache.start();
    }

    @Override
    protected void createInterceptors(InterceptorFactoryContext context) {
        super.createInterceptors(context);
        if(afterBegin != null) {
            afterBeginInterceptor = afterBegin.create(context);
        }
        if(afterCompletion != null) {
            afterCompletionInterceptor = afterCompletion.create(context);
        }
        if(beforeCompletion != null) {
            beforeCompletionInterceptor = beforeCompletion.create(context);
        }
        if(prePassivate != null) {
            prePassivateInterceptor = prePassivate.create(context);
        }
        if(postActivate != null) {
            postActivateInterceptor = postActivate.create(context);
        }
        if(ejb2XRemoveMethod != null) {
            ejb2XRemoveMethodInterceptor = ejb2XRemoveMethod.create(context);
        }
    }

    @Override
    public void done() {
        this.cache.stop();

        cache = null;
        afterBeginInterceptor = null;
        afterCompletionInterceptor = null;
        beforeCompletionInterceptor = null;
        prePassivateInterceptor = null;
        postActivateInterceptor = null;
        ejb2XRemoveMethodInterceptor = null;

        super.done();
    }

    @Override
    public AllowedMethodsInformation getAllowedMethodsInformation() {
        return isBeanManagedTransaction() ? StatefulAllowedMethodsInformation.INSTANCE_BMT : StatefulAllowedMethodsInformation.INSTANCE_CMT;
    }

    public Set<Object> getSerialiableInterceptorContextKeys() {
        return serialiableInterceptorContextKeys;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    public boolean isRemotable(Throwable throwable) {
        return cache.isRemotable(throwable);
    }

    public boolean shouldDiscard(Exception ex, Method method) {

        // Detect app exception
        if (getApplicationException(ex.getClass(), method) != null) {
            // it's an application exception, just throw it back.
            return false;
        }
        if (ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
            return false;
        }
        if (!(ex instanceof RemoveException)) {
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                return true;
            }
        }
        return false;
    }
}
