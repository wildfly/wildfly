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

import java.lang.reflect.Method;
import java.util.List;
import static org.jboss.as.ee.container.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.container.SecurityActions.setContextClassLoader;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.interceptor.InterceptingProxyHandler;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;

/**
 * @author John Bailey
 */
public abstract class AbstractBeanContainer<T> implements BeanContainer<T> {
    final BeanContainerConfig config;
    final List<ResourceInjection> resourceInjections;
    final List<MethodInterceptor> interceptors;

    protected AbstractBeanContainer(BeanContainerConfig config, List<ResourceInjection> resourceInjections, List<MethodInterceptor> interceptors) {
        this.config = config;
        this.resourceInjections = resourceInjections;
        this.interceptors = interceptors;
    }

    /**
     * {@inheritDoc}
     */
    public T getInstance() {
        final T beanInstance = provideBeanInstance(config);
        applyInjections(beanInstance, config);
        performPostConstructLifecycle(beanInstance, config);
        return createBeanProxy(beanInstance);
    }

    /**
     * Provide a bean instance.  By default this will construct a new instance, but this could also be overridden to provide
     * singleton access or to provide a pooled implementation.
     *
     * @param config The container config
     * @return The instance
     */
    protected T provideBeanInstance(final BeanContainerConfig config) {
        final Class<T> beanClass = getBeanClass();
        T instance;
        try {
            instance = beanClass.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate instance of bean: " + beanClass);
        }
        return instance;
    }

    /**
     * Apply the injections to a newly retrieved bean instance.
     *
     * @param instance The bean instance
     * @param config The container config
     */
    protected void applyInjections(final T instance, final BeanContainerConfig config) {
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
     * @param config The container configuration
     */
    protected void performPostConstructLifecycle(final T instance, final BeanContainerConfig config) {
        // Execute the post construct life-cycle
        final Method[] postConstructMethods = config.getPostConstructMethods();
        if (postConstructMethods != null) {
            final ClassLoader contextCl = getContextClassLoader();
            setContextClassLoader(config.getBeanClassLoader());
            try {
                for (Method postConstructMethod : postConstructMethods) {
                    try {
                        postConstructMethod.invoke(instance);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method '" + postConstructMethod.getName() + "' for class " + config.getBeanClass(), t);
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

    @SuppressWarnings("unchecked")
    protected Class<T> getBeanClass() {
        return (Class<T>) config.getBeanClass();
    }

    protected List<MethodInterceptor> getMethodInterceptors() {
        return interceptors;
    }

    /**
     * {@inheritDoc}
     */
    public void returnInstance(final T instance) {
        performPreDestroyLifecycle(instance, config);
    }

    /**
     * Perform any pre-destroy life-cycle routines.  By default it will invoke all pre-destroy methods.
     *
     * @param instance The bean instance
     * @param config The container configuration
     */
    protected void performPreDestroyLifecycle(final T instance, final BeanContainerConfig config) {
        final Method[] preDestroyMethods = config.getPreDestroyMethods();
        if (preDestroyMethods == null) {
            return;
        }
        // Execute the post construct life-cycle
        final ClassLoader contextCl = getContextClassLoader();
        setContextClassLoader(config.getBeanClassLoader());
        try {
            for (Method preDestroyMethod : preDestroyMethods) {
                try {
                    preDestroyMethod.invoke(instance);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to invoke pre destroy method '" + preDestroyMethod.getName() + "' for class " + config.getBeanClass(), t);
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
