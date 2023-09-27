/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ContextClassLoaderInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class StatefulSessionComponentCreateService extends SessionBeanComponentCreateService {

    private final InterceptorFactory afterBegin;
    private final Method afterBeginMethod;
    private final InterceptorFactory afterCompletion;
    private final Method afterCompletionMethod;
    private final InterceptorFactory beforeCompletion;
    private final Method beforeCompletionMethod;
    private final InterceptorFactory prePassivate;
    private final InterceptorFactory postActivate;
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();
    private final InjectedValue<AtomicLong> defaultStatefulSessionTimeoutValue = new InjectedValue<>();
    private final InterceptorFactory ejb2XRemoveMethod;
    private final Supplier<StatefulSessionBeanCacheFactory<SessionID, StatefulSessionComponentInstance>> cacheFactory;
    private final Set<Object> serializableInterceptorContextKeys;
    private final StatefulComponentDescription componentDescription;
    final boolean passivationCapable;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration, Supplier<StatefulSessionBeanCacheFactory<SessionID, StatefulSessionComponentInstance>> cacheFactory) {
        super(componentConfiguration, ejbJarConfiguration);

        final StatefulComponentDescription componentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
        final ClassLoader classLoader = componentConfiguration.getModuleClassLoader();
        final InterceptorFactory tcclInterceptorFactory = new ImmediateInterceptorFactory(new ContextClassLoaderInterceptor(classLoader));
        final InterceptorFactory namespaceContextInterceptorFactory = componentConfiguration.getNamespaceContextInterceptorFactory();

        this.afterBeginMethod = componentDescription.getAfterBegin();
        this.afterBegin = (this.afterBeginMethod != null) ? Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(this.afterBeginMethod)) : null;
        this.afterCompletionMethod = componentDescription.getAfterCompletion();
        this.afterCompletion = (this.afterCompletionMethod != null) ? Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(this.afterCompletionMethod)) : null;
        this.beforeCompletionMethod = componentDescription.getBeforeCompletion();
        this.beforeCompletion = (this.beforeCompletionMethod != null) ? Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(this.beforeCompletionMethod)) : null;
        this.prePassivate = Interceptors.getChainedInterceptorFactory(componentConfiguration.getPrePassivateInterceptors());
        this.postActivate = Interceptors.getChainedInterceptorFactory(componentConfiguration.getPostActivateInterceptors());
        //the interceptor chain for EJB e.x remove methods
        this.ejb2XRemoveMethod = Interceptors.getChainedInterceptorFactory(StatefulSessionSynchronizationInterceptor.factory(componentDescription.getTransactionManagementType()), new ImmediateInterceptorFactory(new StatefulRemoveInterceptor(false)), Interceptors.getTerminalInterceptorFactory());
        this.serializableInterceptorContextKeys = componentConfiguration.getInterceptorContextKeys();
        this.passivationCapable = componentDescription.isPassivationApplicable();
        this.cacheFactory = cacheFactory;
        this.componentDescription = componentDescription;
    }

    private static InterceptorFactory invokeMethodOnTarget(final Method method) {
        method.setAccessible(true);
        return InvokeMethodOnTargetInterceptor.factory(method);
    }

    @Override
    protected BasicComponent createComponent() {
        return new StatefulSessionComponent(this);
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

    public InterceptorFactory getPrePassivate() {
        return this.prePassivate;
    }

    public InterceptorFactory getPostActivate() {
        return this.postActivate;
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

    public DefaultAccessTimeoutService getDefaultAccessTimeoutService() {
        return defaultAccessTimeoutService.getValue();
    }

    Injector<DefaultAccessTimeoutService> getDefaultAccessTimeoutInjector() {
        return this.defaultAccessTimeoutService;
    }

    Injector<AtomicLong> getDefaultStatefulSessionTimeoutInjector() {
        return this.defaultStatefulSessionTimeoutValue;
    }

    void setDefaultStatefulSessionTimeout() {
        if (componentDescription.getStatefulTimeout() == null) {
            final long timeoutVal = defaultStatefulSessionTimeoutValue.getValue().get();
            if (timeoutVal >= 0) {
                componentDescription.setStatefulTimeout(new StatefulTimeoutInfo(timeoutVal, TimeUnit.MILLISECONDS));
            }
        }
    }

    public InterceptorFactory getEjb2XRemoveMethod() {
        return ejb2XRemoveMethod;
    }

    public Set<Object> getSerializableInterceptorContextKeys() {
        return serializableInterceptorContextKeys;
    }

    Supplier<StatefulSessionBeanCacheFactory<SessionID, StatefulSessionComponentInstance>> getCacheFactory() {
        return this.cacheFactory;
    }
}
