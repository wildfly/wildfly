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
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.invocation.ContextClassLoaderInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.Time;

/**
 * @author Stuart Douglas
 */
public class StatefulSessionComponentCreateService extends SessionBeanComponentCreateService implements BeanContext {

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
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();
    private final InterceptorFactory ejb2XRemoveMethod;
    private final Value<CacheFactory> cacheFactory;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<CacheFactoryBuilder> cacheFactoryBuilder = new InjectedValue<>();
    private final Set<Object> serializableInterceptorContextKeys;
    final boolean passivationCapable;
    private final ModuleLoader moduleLoader;
    private final ServiceName deploymentUnitServiceName;
    private final ServiceName componentServiceName;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration, Value<CacheFactory> cacheFactory) {
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
        this.moduleLoader = componentConfiguration.getModuleLoader();
        this.serializableInterceptorContextKeys = componentConfiguration.getInterceptorContextKeys();
        this.passivationCapable = componentDescription.isPassivationApplicable();
        this.deploymentUnitServiceName = componentDescription.getDeploymentUnitServiceName();
        this.componentServiceName = componentDescription.getServiceName();
        this.cacheFactory = cacheFactory;
    }

    private static InterceptorFactory invokeMethodOnTarget(final Method method) {
        method.setAccessible(true);
        return InvokeMethodOnTargetInterceptor.factory(method);
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        this.cacheFactoryBuilder.getValue().build(context.getChildTarget(), this.componentServiceName.append("cache"), this, this.statefulTimeout).install();
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
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

    public CacheInfo getCache() {
        return this.cache;
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

    @SuppressWarnings("rawtypes")
    Injector<CacheFactoryBuilder> getCacheFactoryBuilderInjector() {
        return this.cacheFactoryBuilder;
    }

    @SuppressWarnings("rawtypes")
    Value<CacheFactory> getCacheFactory() {
        return this.cacheFactory;
    }

    boolean isPassivationCapable() {
        return this.passivationCapable;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.loader;
    }

    @Override
    public String getBeanName() {
        return this.getComponentName();
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this.moduleLoader;
    }

    @Override
    public ServiceName getDeploymentUnitServiceName() {
        return this.deploymentUnitServiceName;
    }

    @Override
    public Time getTimeout() {
        return (this.statefulTimeout != null) ? new Time(this.statefulTimeout.getValue(), this.statefulTimeout.getTimeUnit()) : null;
    }
}
