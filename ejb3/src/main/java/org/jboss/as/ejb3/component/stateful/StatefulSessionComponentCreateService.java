/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.cache.impl.factory.NonPassivatingCacheFactory;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ContextClassLoaderInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class StatefulSessionComponentCreateService extends SessionBeanComponentCreateService {
    private static final int CURRENT_MARSHALLING_VERSION = 1;

    private final InterceptorFactory afterBegin;
    private final Method afterBeginMethod;
    private final InterceptorFactory afterCompletion;
    private final Method afterCompletionMethod;
    private final InterceptorFactory beforeCompletion;
    private final Method beforeCompletionMethod;
    private final InterceptorFactory prePassivate;
    private final InterceptorFactory postActivate;
    private final StatefulTimeoutInfo statefulTimeout;
    private final CacheInfo cache;
    private final ClassLoader loader;
    private final Map<Integer, MarshallingConfiguration> marshallingConfigurations;
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();
    private final InterceptorFactory ejb2XRemoveMethod;
    @SuppressWarnings("rawtypes")
    private final PassivationCheckInjectedValue cacheFactory = new PassivationCheckInjectedValue(new InjectedValue<CacheFactory>());
    private final Set<Object> serializableInterceptorContextKeys;
    private final boolean passivationCapable;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
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
        this.statefulTimeout = componentDescription.getStatefulTimeout();
        //the interceptor chain for EJB e.x remove methods
        this.ejb2XRemoveMethod = Interceptors.getChainedInterceptorFactory(StatefulSessionSynchronizationInterceptor.factory(componentDescription.getTransactionManagementType()), new ImmediateInterceptorFactory(new StatefulRemoveInterceptor(false)), Interceptors.getTerminalInterceptorFactory());
        this.cache = componentDescription.getCache();
        this.loader = componentConfiguration.getModuleClassLoader();
        MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setClassResolver(ModularClassResolver.getInstance(componentConfiguration.getModuleLoader()));
        marshallingConfiguration.setSerializabilityChecker(new StatefulSessionBeanSerializabilityChecker(componentConfiguration.getComponentClass()));
        marshallingConfiguration.setClassTable(new StatefulSessionBeanClassTable());
        // ObjectTable which handles serialization of EJB proxies
        marshallingConfiguration.setObjectTable(new EJBClientContextIdentifierObjectTable());
        this.marshallingConfigurations = Collections.singletonMap(CURRENT_MARSHALLING_VERSION, marshallingConfiguration);
        this.serializableInterceptorContextKeys = componentConfiguration.getInterceptorContextKeys();
        this.passivationCapable = componentDescription.isPassivationApplicable();
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

    public StatefulTimeoutInfo getStatefulTimeout() {
        return statefulTimeout;
    }

    public CacheInfo getCache() {
        return this.cache;
    }

    public ClassLoader getClassLoader() {
        return this.loader;
    }

    public int getCurrentMarshallingVersion() {
        return CURRENT_MARSHALLING_VERSION;
    }

    public Map<Integer, MarshallingConfiguration> getMarshallingConfigurations() {
        return this.marshallingConfigurations;
    }

    public DefaultAccessTimeoutService getDefaultAccessTimeoutService() {
        return defaultAccessTimeoutService.getValue();
    }

    Injector<DefaultAccessTimeoutService> getDefaultAccessTimeoutInjector() {
        return this.defaultAccessTimeoutService;
    }

    public InterceptorFactory getEjb2XRemoveMethod() {
        return ejb2XRemoveMethod;
    }

    public Set<Object> getSerializableInterceptorContextKeys() {
        return serializableInterceptorContextKeys;
    }

    @SuppressWarnings("unchecked")
    public CacheFactory<SessionID, StatefulSessionComponentInstance> getCacheFactory() {
        return (CacheFactory<SessionID, StatefulSessionComponentInstance>) this.cacheFactory.getValue();
    }

    @SuppressWarnings("rawtypes")
    Injector<CacheFactory> getCacheFactoryInjector() {
        return this.cacheFactory;
    }

    boolean isPassivationCapable() {
        return this.passivationCapable;
    }

    /**
     * An {@link Injector} and a {@link Value} which is used to check if the stateful bean component is disabled for passivation
     * and if it is then the cache factory being injected doesn't have a passivation store associated with it.
     */
    @SuppressWarnings("rawtypes")
    private class PassivationCheckInjectedValue implements Injector<CacheFactory>, Value<CacheFactory> {

        private final InjectedValue<CacheFactory> delegate;

        PassivationCheckInjectedValue(final InjectedValue<CacheFactory> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void inject(CacheFactory value) throws InjectionException {
            // if the SFSB is disabled for passivation then the backing cache factory isn't allowed to have a passivation store.
            // For now we rely on the CacheFactory class type to see if the cache factory is enabled/disabled for passivation,
            // since there are only a couple of cache factory types (NonPassivatingCacheFactory is one of them) and there isn't
            // a specific, straightforward API for getting that information from the cache factory
            if (!passivationCapable && !(value instanceof NonPassivatingCacheFactory)) {
                throw EjbMessages.MESSAGES.requiresNonPassivatingCacheFactory(getComponentName(), value);
            }
            this.delegate.inject(value);
        }

        @Override
        public void uninject() {
            this.delegate.uninject();
        }

        @Override
        public CacheFactory getValue() throws IllegalStateException, IllegalArgumentException {
            return this.delegate.getValue();
        }
    }
}
