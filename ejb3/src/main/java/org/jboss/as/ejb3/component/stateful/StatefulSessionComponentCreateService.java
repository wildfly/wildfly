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

import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.as.clustering.ClassLoaderAwareClassResolver;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.TCCLInterceptor;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
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
    private final StatefulTimeoutInfo statefulTimeout;
    private final CacheInfo cache;
    private final MarshallingConfiguration marshallingConfiguration;
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();
    private final InterceptorFactory ejb2XRemoveMethod;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<CacheFactory> cacheFactory = new InjectedValue<CacheFactory>();
    private final Set<Object> serializableInterceptorContextKeys;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final StatefulComponentDescription componentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
        final InterceptorFactory tcclInterceptorFactory = new ImmediateInterceptorFactory(new TCCLInterceptor(componentConfiguration.getModuleClassLoder()));
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
        this.marshallingConfiguration = new MarshallingConfiguration();
        this.marshallingConfiguration.setSerializedCreator(new SunReflectiveCreator());
        this.marshallingConfiguration.setExternalizerCreator(new ReflectiveCreator());
        this.marshallingConfiguration.setClassResolver(new ClassLoaderAwareClassResolver(ModularClassResolver.getInstance(componentConfiguration.getModuleLoader()), componentConfiguration.getModuleClassLoder()));
        this.marshallingConfiguration.setSerializabilityChecker(new StatefulSessionBeanSerializabilityChecker(componentConfiguration.getComponentClass()));
        this.marshallingConfiguration.setClassTable(new StatefulSessionBeanClassTable());
        this.serializableInterceptorContextKeys = componentConfiguration.getInterceptorContextKeys();
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

    public MarshallingConfiguration getMarshallingConfiguration() {
        return this.marshallingConfiguration;
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
}
