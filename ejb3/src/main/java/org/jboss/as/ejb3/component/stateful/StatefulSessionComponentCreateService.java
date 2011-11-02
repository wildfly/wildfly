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

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.TCCLInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
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
    private final StatefulTimeoutInfo statefulTimeout;
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final StatefulComponentDescription componentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
        final InterceptorFactory tcclInterceptorFactory = new ImmediateInterceptorFactory(new TCCLInterceptor(componentConfiguration.getComponentClass().getClassLoader()));
        final InterceptorFactory namespaceContextInterceptorFactory = componentConfiguration.getNamespaceContextInterceptorFactory();
        this.afterBeginMethod = componentDescription.getAfterBegin();
        if (componentDescription.getAfterBegin() != null) {
            this.afterBegin = Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(componentDescription.getAfterBegin()));
        } else {
            this.afterBegin = null;
        }
        this.afterCompletionMethod = componentDescription.getAfterCompletion();
        if (componentDescription.getAfterCompletion() != null) {
            this.afterCompletion = Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(componentDescription.getAfterCompletion()));
        } else {
            this.afterCompletion = null;
        }
        this.beforeCompletionMethod = componentDescription.getBeforeCompletion();
        if (componentDescription.getBeforeCompletion() != null) {
            this.beforeCompletion = Interceptors.getChainedInterceptorFactory(tcclInterceptorFactory, namespaceContextInterceptorFactory, CurrentInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(componentDescription.getBeforeCompletion()));
        } else {
            this.beforeCompletion = null;
        }
        this.statefulTimeout = componentDescription.getStatefulTimeout();
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

    public DefaultAccessTimeoutService getDefaultAccessTimeoutService() {
        return defaultAccessTimeoutService.getValue();
    }

    Injector<DefaultAccessTimeoutService> getDefaultAccessTimeoutInjector() {
        return this.defaultAccessTimeoutService;
    }
}
