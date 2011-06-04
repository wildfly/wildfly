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
import org.jboss.as.ee.component.TCCLInterceptor;
import org.jboss.as.ejb3.PrimitiveClassLoaderUtil;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.component.session.SessionInvocationContextInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Stuart Douglas
 */
public class StatefulSessionComponentCreateService extends SessionBeanComponentCreateService {
    private final InterceptorFactory afterBegin;
    private final InterceptorFactory afterCompletion;
    private final InterceptorFactory beforeCompletion;
    private final StatefulTimeoutInfo statefulTimeout;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatefulSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final EjbJarConfiguration ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final StatefulComponentDescription componentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
        final InterceptorFactory tcclInterceptorFactory = new ImmediateInterceptorFactory(new TCCLInterceptor(componentConfiguration.getComponentClass().getClassLoader()));
        final InterceptorFactory namespaceContextInterceptorFactory = componentConfiguration.getNamespaceContextInterceptorFactory();
        final Class<?> beanClass = componentConfiguration.getComponentClass();
        this.afterBegin = interceptorFactoryChain(tcclInterceptorFactory, namespaceContextInterceptorFactory, SessionInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(beanClass, componentDescription.getAfterBegin()));
        this.afterCompletion = interceptorFactoryChain(tcclInterceptorFactory, namespaceContextInterceptorFactory, SessionInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(beanClass, componentDescription.getAfterCompletion()));
        this.beforeCompletion = interceptorFactoryChain(tcclInterceptorFactory, namespaceContextInterceptorFactory, SessionInvocationContextInterceptor.FACTORY, invokeMethodOnTarget(beanClass, componentDescription.getBeforeCompletion()));
        this.statefulTimeout = componentDescription.getStatefulTimeout();
    }

    private static InterceptorFactory invokeMethodOnTarget(Class<?> beanClass, MethodDescription methodDescription) {
        final Method method = methodOf(beanClass, methodDescription);
        if (method == null)
            return null;
        method.setAccessible(true);
        return InvokeMethodOnTargetInterceptor.factory(method);
    }

    private static InterceptorFactory interceptorFactoryChain(final InterceptorFactory... factories) {
        // a little bit of magic
        if (factories[factories.length - 1] == null)
            return null;
        return Interceptors.getChainedInterceptorFactory(factories);
    }

    @Override
    protected BasicComponent createComponent() {
        return new StatefulSessionComponent(this);
    }

    private static Method declaredMethodOfHierarchy(final Class<?> cls, final String name, final Class<?>[] parameterTypes) {
        if (cls == null)
            throw new RuntimeException("Unable to find method " + name + " " + Arrays.toString(parameterTypes));
        try {
            return cls.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return declaredMethodOfHierarchy(cls.getSuperclass(), name, parameterTypes);
        }
    }

    private static Method methodOf(Class<?> cls, MethodDescription methodDescription) {
        if (methodDescription == null)
            return null;
        try {
            final ClassLoader classLoader = cls.getClassLoader();
            final String[] types = methodDescription.params;
            final Class<?>[] paramTypes = new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
                paramTypes[i] = PrimitiveClassLoaderUtil.loadClass(types[i], classLoader);
            }
            if (methodDescription.className != null) {
                final Class<?> declaringClass = Class.forName(methodDescription.className, false, classLoader);
                return declaringClass.getDeclaredMethod(methodDescription.methodName, paramTypes);
            }
            return declaredMethodOfHierarchy(cls, methodDescription.methodName, paramTypes);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public StatefulTimeoutInfo getStatefulTimeout() {
        return statefulTimeout;
    }
}
