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

package org.jboss.as.ee.container;

import java.util.List;
import static org.jboss.as.ee.container.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.container.SecurityActions.setContextClassLoader;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.interceptor.InterceptingProxyHandler;
import org.jboss.as.ee.container.interceptor.LifecycleInterceptor;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;

/**
 * @author John Bailey
 */
public abstract class AbstractBeanContainer<T> implements BeanContainer<T> {
    private final Class<T> beanClass;
    private final ClassLoader beanClassLoader;
    private final List<ResourceInjection> resourceInjections;
    private final List<LifecycleInterceptor> postConstrucInterceptors;
    private final List<LifecycleInterceptor> preDestroyInterceptors;
    private final List<MethodInterceptor> methodInterceptors;

    protected AbstractBeanContainer(Class<T> beanClass, ClassLoader beanClassLoader, List<ResourceInjection> resourceInjections, List<LifecycleInterceptor> postConstrucInterceptors, List<LifecycleInterceptor> preDestroyInterceptors, List<MethodInterceptor> methodInterceptors) {
        this.beanClass = beanClass;
        this.beanClassLoader = beanClassLoader;
        this.resourceInjections = resourceInjections;
        this.postConstrucInterceptors = postConstrucInterceptors;
        this.preDestroyInterceptors = preDestroyInterceptors;
        this.methodInterceptors = methodInterceptors;
    }

    /**
     * {@inheritDoc}
     */
    public T getInstance() {
        final T beanInstance = provideBeanInstance();
        applyInjections(beanInstance);
        performPostConstructLifecycle(beanInstance);
        return createBeanProxy(beanInstance);
    }

    /**
     * Provide a bean instance.  By default this will construct a new instance, but this could also be overridden to provide
     * singleton access or to provide a pooled implementation.
     *
     * @return The instance
     */
    protected T provideBeanInstance() {
        final Class<T> beanClass = getBeanClass();
        T instance;
        try {
            instance = beanClass.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate instance of bean: " + beanClass);
        }
        return instance;
    }

    protected Class<T> getBeanClass() {
        return beanClass;
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
        final List<LifecycleInterceptor> postConstructMethods = postConstrucInterceptors;
        if (postConstructMethods != null && !postConstructMethods.isEmpty()) {
            final ClassLoader contextCl = getContextClassLoader();
            setContextClassLoader(beanClassLoader);
            try {
                for (LifecycleInterceptor postConstructMethod : postConstructMethods) {
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

    /**
     * Create the bean proxy.
     *
     * @param instance The bean instance
     * @return The proxy
     */
    protected T createBeanProxy(final T instance) {
        return InterceptingProxyHandler.createProxy(getBeanClass(), instance, getMethodInterceptors());
    }

    protected List<MethodInterceptor> getMethodInterceptors() {
        return methodInterceptors;
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
        final List<LifecycleInterceptor> preDestroyInterceptors = this.preDestroyInterceptors;
        if (preDestroyInterceptors == null || !preDestroyInterceptors.isEmpty()) {
            return;
        }
        // Execute the post construct life-cycle
        final ClassLoader contextCl = getContextClassLoader();
        setContextClassLoader(beanClassLoader);
        try {
            for (LifecycleInterceptor preDestroyMethod : preDestroyInterceptors) {
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
}
