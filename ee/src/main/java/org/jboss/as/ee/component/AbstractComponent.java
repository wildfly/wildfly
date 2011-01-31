/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import static org.jboss.as.ee.component.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.component.SecurityActions.setContextClassLoader;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.interceptor.ComponentInstanceInterceptor;
import org.jboss.as.ee.component.interceptor.ContextSelectorInterceptor;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorInvocationHandler;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * @author John Bailey
 */
public abstract class AbstractComponent<T> implements Component<T> {
    private final Class<T> beanClass;
    private final ClassLoader beanClassLoader;
    private final List<ResourceInjection> resourceInjections;
    private final List<ComponentLifecycle> postConstrucInterceptors;
    private final List<ComponentLifecycle> preDestroyInterceptors;
    private final Map<Method, InterceptorFactory> methodInterceptorFactories;
    private final ProxyFactory<T> proxyFactory;

    private Context applicationContext;
    private Context moduleContext;
    private Context componentContext;

    protected AbstractComponent(final Class<T> beanClass, final ClassLoader beanClassLoader, final List<ResourceInjection> resourceInjections, final List<ComponentLifecycle> postConstrucInterceptors, final List<ComponentLifecycle> preDestroyInterceptors, final Map<Method, InterceptorFactory> methodInterceptorFactories) {
        this.beanClass = beanClass;
        this.beanClassLoader = beanClassLoader;
        this.resourceInjections = resourceInjections;
        this.postConstrucInterceptors = postConstrucInterceptors;
        this.preDestroyInterceptors = preDestroyInterceptors;
        this.methodInterceptorFactories = methodInterceptorFactories;
        this.proxyFactory = new ProxyFactory<T>(beanClass);
    }

    /**
     * {@inheritDoc}
     */
    public T getInstance() {
        final Class<T> beanClass = getBeanClass();
        T instance;
        try {
            instance = beanClass.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate instance of bean: " + beanClass);
        }
        applyInjections(instance);
        performPostConstructLifecycle(instance);
        return instance;
    }

    protected Class<T> getBeanClass() {
        return beanClass;
    }

    /**
     * {@inheritDoc}
     */
    public T createProxy() {
        final Interceptor defaultChain = Interceptors.getChainedInterceptor(getComponentLevelInterceptors());
        try {
            return proxyFactory.newInstance(new InterceptorInvocationHandler(defaultChain));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy instance for bean component: " + beanClass, e);
        }
    }

    /**
     * Apply the injections to a newly retrieved bean instance.
     *
     * @param instance The bean instance
     */
    protected void applyInjections(final T instance) {
        for (ResourceInjection resourceInjection : getResourceInjections()) {
            resourceInjection.inject(instance);
        }
    }

    protected List<ResourceInjection> getResourceInjections() {
        return resourceInjections;
    }

    /**
     * Perform any post-construct life-cycle routines.  By default this will run any post-construct methods.
     *
     * @param instance The bean instance
     */
    protected void performPostConstructLifecycle(final T instance) {
        final List<ComponentLifecycle> postConstructMethods = postConstrucInterceptors;
        if (postConstructMethods != null && !postConstructMethods.isEmpty()) {
            final ClassLoader contextCl = getContextClassLoader();
            setContextClassLoader(beanClassLoader);
            try {
                for (ComponentLifecycle postConstructMethod : postConstructMethods) {
                    try {
                        postConstructMethod.invoke(instance);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method for class " + getBeanClass(), t);
                    }
                }
            } finally {
                setContextClassLoader(contextCl);
            }
        }
    }

    protected Map<Method, InterceptorFactory> getMethodInterceptorFactories() {
        return methodInterceptorFactories;
    }

    protected List<Interceptor> getComponentLevelInterceptors() {
        final List<Interceptor> componentLevelInterceptors = new ArrayList<Interceptor>();
        componentLevelInterceptors.add(new ContextSelectorInterceptor(this));

        // TODO:  Figure out how to route the rest in.

        // Add the instance interceptor last
        componentLevelInterceptors.add(new ComponentInstanceInterceptor<T>(this, getMethodInterceptorFactories()));
        return componentLevelInterceptors;
    }

    /**
     * {@inheritDoc}
     */
    public void returnInstance(final T instance) {
        performPreDestroyLifecycle(instance);
        applyUninjections(instance);
    }

    protected void applyUninjections(final T instance) {
        for (ResourceInjection resourceInjection : getResourceInjections()) {
            resourceInjection.uninject(instance);
        }
    }

    /**
     * Perform any pre-destroy life-cycle routines.  By default it will invoke all pre-destroy methods.
     *
     * @param instance The bean instance
     */
    protected void performPreDestroyLifecycle(final T instance) {
        final List<ComponentLifecycle> preDestroyInterceptors = this.preDestroyInterceptors;
        if (preDestroyInterceptors == null || !preDestroyInterceptors.isEmpty()) {
            return;
        }
        // Execute the post construct life-cycle
        final ClassLoader contextCl = getContextClassLoader();
        setContextClassLoader(beanClassLoader);
        try {
            for (ComponentLifecycle preDestroyMethod : preDestroyInterceptors) {
                try {
                    preDestroyMethod.invoke(instance);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to invoke pre destroy method for class " + getBeanClass(), t);
                }
            }
        } finally {
            setContextClassLoader(contextCl);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
    }

    /**
     * {@inheritDoc}
     */
    public Context getComponentContext() {
        return componentContext;
    }

    /**
     * {@inheritDoc}
     */
    public Context getModuleContext() {
        return moduleContext;
    }

    /**
     * {@inheritDoc}
     */
    public Context getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setModuleContext(Context moduleContext) {
        this.moduleContext = moduleContext;
    }

    public void setComponentContext(Context componentContext) {
        this.componentContext = componentContext;
    }
}
