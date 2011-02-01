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

import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import static org.jboss.as.ee.component.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.component.SecurityActions.setContextClassLoader;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.interceptor.ComponentInstanceInterceptor;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.interceptor.ContextSelectorInterceptor;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * @author John Bailey
 */
public abstract class AbstractComponent implements Component {
    private final Class<?> beanClass;
    private final ClassLoader beanClassLoader;
    private final List<ResourceInjection> resourceInjections;
    private final List<ComponentLifecycle> postConstructInterceptors;
    private final List<ComponentLifecycle> preDestroyInterceptors;
    private final ComponentInterceptorFactories methodInterceptorFactories;
    private final ProxyFactory<?> proxyFactory;
    private Context applicationContext;
    private Context moduleContext;
    private Context componentContext;

    private final NamespaceContextSelector selector = new NamespaceContextSelector() {
        public Context getContext(String identifier) {
            if (identifier.equals("comp")) {
                return componentContext;
            } else if (identifier.equals("module")) {
                return moduleContext;
            } else if (identifier.equals("app")) {
                return applicationContext;
            }
            return null;
        }
    };


    protected AbstractComponent(final Class<?> beanClass, final ClassLoader beanClassLoader, final List<ResourceInjection> resourceInjections, final List<ComponentLifecycle> postConstructInterceptors, final List<ComponentLifecycle> preDestroyInterceptors, final ComponentInterceptorFactories methodInterceptorFactories) {
        this.beanClass = beanClass;
        this.beanClassLoader = beanClassLoader;
        this.resourceInjections = resourceInjections;
        this.postConstructInterceptors = postConstructInterceptors;
        this.preDestroyInterceptors = preDestroyInterceptors;
        this.methodInterceptorFactories = methodInterceptorFactories;
        this.proxyFactory = createProxyFactory(beanClass);
    }

    private static <T> ProxyFactory<T> createProxyFactory(Class<T> type) {
        return new ProxyFactory<T>(type);
    }

    /**
     * Get the instance.  If the component implementation wishes to apply a pooling policy or something similar,
     * then override this method to return the pooled instance; if a new instance is then desired, call up to this
     * method implementation to get a new instance which has injections and lifecycle methods applied to it.
     */
    public ComponentInstance getInstance() {
        NamespaceContextSelector.pushCurrentSelector(selector);
        try {
            Object objectInstance = createObjectInstance();
            applyInjections(objectInstance);
            performPostConstructLifecycle(objectInstance);
            return createComponentInstance(objectInstance);
        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }

    /**
     * Create a new instance.
     *
     * @return the new instance
     */
    protected Object createObjectInstance() {
        // TODO: provide a plug point for CDI instantiator
        try {
            return beanClass.newInstance();
        } catch (InstantiationException e) {
            InstantiationError error = new InstantiationError(e.getMessage());
            error.setStackTrace(e.getStackTrace());
            throw error;
        } catch (IllegalAccessException e) {
            IllegalAccessError error = new IllegalAccessError(e.getMessage());
            error.setStackTrace(e.getStackTrace());
            throw error;
        }
    }

    /**
     * Construct the component instance.
     *
     * @param instance the object instance to wrap
     * @return the component instance
     */
    protected abstract AbstractComponentInstance createComponentInstance(Object instance);

    /**
     * Get the class of this bean component.
     *
     * @return the class
     */
    protected Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Apply the injections to a newly retrieved bean instance.
     *
     * @param instance The bean instance
     */
    protected void applyInjections(final Object instance) {
        final List<ResourceInjection> resourceInjections = getResourceInjections();
        if(resourceInjections != null) {
            for (ResourceInjection resourceInjection : resourceInjections) {
                resourceInjection.inject(instance);
            }
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
    protected void performPostConstructLifecycle(final Object instance) {
        final List<ComponentLifecycle> postConstructMethods = postConstructInterceptors;
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

    protected ComponentInterceptorFactories getMethodInterceptorFactories() {
        return methodInterceptorFactories;
    }

    protected List<Interceptor> getComponentLevelInterceptors() {
        final List<Interceptor> componentLevelInterceptors = new ArrayList<Interceptor>();
        componentLevelInterceptors.add(new ContextSelectorInterceptor(this));

        // TODO:  Figure out how to route the rest in.
        return componentLevelInterceptors;
    }

    /**
     * {@inheritDoc}
     */
    public void returnInstance(final ComponentInstance instance) {
        performPreDestroyLifecycle(instance);
        applyUninjections(instance);
    }

    public NamespaceContextSelector getNamespaceContextSelector() {
        return selector;
    }

    protected void applyUninjections(final Object instance) {
        for (ResourceInjection resourceInjection : getResourceInjections()) {
            resourceInjection.uninject(instance);
        }
    }

    /**
     * Perform any pre-destroy life-cycle routines.  By default it will invoke all pre-destroy methods.
     *
     * @param instance The bean instance
     */
    protected void performPreDestroyLifecycle(final Object instance) {
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

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setModuleContext(Context moduleContext) {
        this.moduleContext = moduleContext;
    }

    public void setComponentContext(Context componentContext) {
        this.componentContext = componentContext;
    }

    protected ProxyFactory<?> getProxyFactory() {
        return proxyFactory;
    }
}
