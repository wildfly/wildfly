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

package org.jboss.as.managedbean.container;

import static org.jboss.as.managedbean.container.SecurityActions.getContextClassLoader;
import static org.jboss.as.managedbean.container.SecurityActions.setContextClassLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Container responsible for holding onto the components necessary for creating instance of managed beans.
 *
 * @param <T> The managed bean object type
 *
 * @author John E. Bailey
 */
public class ManagedBeanContainer<T> {
    private final Class<T> beanClass;
    private final ClassLoader deploymentClassLoader;
    private final List<Method> postConstructMethods;
    private final List<Method> preDestroyMethods;
    private final List<ResourceInjection<?>> resourceInjections;
    private final List<ManagedBeanInterceptor<?>> interceptors;

    /**
     * Construct with managed bean configuration.
     *
     * @param beanClass The class of the managed bean
     * @param deploymentClassLoader The classloader for the deployment
     * @param postConstructMethods The post construct methods
     * @param preDestroyMethods The pre destroy methods
     * @param resourceInjections The resource injections
     * @param interceptors The manged bean interceptors
     */
    public ManagedBeanContainer(final Class<T> beanClass, final ClassLoader deploymentClassLoader, final List<Method> postConstructMethods, final List<Method> preDestroyMethods, final List<ResourceInjection<?>> resourceInjections, final List<ManagedBeanInterceptor<?>> interceptors) {
        this.beanClass = beanClass;
        this.deploymentClassLoader = deploymentClassLoader;
        this.postConstructMethods = postConstructMethods;
        this.preDestroyMethods = preDestroyMethods;
        this.resourceInjections = resourceInjections;
        this.interceptors = interceptors;
    }

    /**
     * Create a new instance of the managed bean.  This will return a new instance of the managed bean will all the injections
     * complete and the post-construct methods called.
     *
     * @return A new instance of the managed bean.
     */
    public T createInstance() {
        // Create instance
        T managedBean;
        try {
            managedBean = beanClass.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate instance of MangedBean: " + beanClass);
        }
        // Execute the injections
        for (ResourceInjection<?> resourceInjection : resourceInjections) {
            resourceInjection.inject(managedBean);
        }
        // Execute the post construct life-cycle
        final ClassLoader contextCl = getContextClassLoader();
        setContextClassLoader(deploymentClassLoader);
        try {
            if (postConstructMethods != null) {
                for(Method postConstructMethod : postConstructMethods) {
                    try {
                        postConstructMethod.invoke(managedBean);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method '" + postConstructMethod.getName() + "' for class " + beanClass, t);
                    }
                }
            }
        } finally {
            setContextClassLoader(contextCl);
        }

        if(!interceptors.isEmpty()) {
            // Create a proxy
            final List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> aroundInvokeInterceptors = new ArrayList<ManagedBeanInterceptor.AroundInvokeInterceptor<?>>(interceptors.size());
            for(ManagedBeanInterceptor<?> managedBeanInterceptor : interceptors) {
                try {
                    aroundInvokeInterceptors.add(managedBeanInterceptor.createInstance());
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to create instance of interceptor " + managedBeanInterceptor.toString(), t);
                }
            }
            try {
                managedBean = ManagedBeanProxyHandler.createProxy(beanClass, deploymentClassLoader, managedBean, aroundInvokeInterceptors);
            } catch (Throwable t) {
                throw new RuntimeException("Unable to create managed bean proxy for " + beanClass, t);
            }
        }
        return managedBean;
    }


}
