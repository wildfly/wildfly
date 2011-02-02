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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static org.jboss.as.ee.component.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.component.SecurityActions.setContextClassLoader;

import java.util.Map;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.CannotProceedException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.InterceptorInstanceFactory;
import org.jboss.invocation.InterceptorInvocationHandler;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.MethodInvokingInterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * The parent of all component classes.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponent implements Component {

    static final Object INSTANCE_KEY = new Object();

    private static final InterceptorInstanceFactory INSTANCE_FACTORY = new InterceptorInstanceFactory() {
        public Object createInstance(final InterceptorFactoryContext context) {
            return context.getContextData().get(INSTANCE_KEY);
        }
    };
    private static final Interceptor DISPATCHER = new Interceptor() {
        public Object processInvocation(final InterceptorContext context) throws Exception {
            // Get the appropriate method from the previously associated instance
            final ComponentInstance instance = context.getPrivateData(ComponentInstance.class);
            if (instance == null) {
                throw new CannotProceedException("No instance is associated with this component class");
            }
            context.setTarget(instance.getInstance());
            final Interceptor interceptor = instance.getInterceptor(context.getMethod());
            return interceptor.processInvocation(context);
        }
    };

    private final Class<?> componentClass;
    private final List<ResourceInjection> resourceInjections;
    private final List<ComponentLifecycle> postConstructInterceptors;
    private final List<ComponentLifecycle> preDestroyInterceptors;
    private final Map<Class<?>, InvocationHandler> views;
    private final Map<Method, InterceptorFactory> interceptorFactoryMap;
    private final Interceptor componentInterceptor;

    // initialized later
    private javax.naming.Context applicationContext;
    private javax.naming.Context moduleContext;
    private javax.naming.Context componentContext;

    private final NamespaceContextSelector selector = new NamespaceContextSelector() {
        public javax.naming.Context getContext(String identifier) {
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

    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     * @param deploymentClassLoader the class loader of the deployment
     * @param index the deployment reflection index
     */
    protected AbstractComponent(final ComponentConfiguration configuration, final ClassLoader deploymentClassLoader, final DeploymentReflectionIndex index) {
        componentClass = configuration.getComponentClass();
        resourceInjections = configuration.getResourceInjections();
        postConstructInterceptors = configuration.getPostConstructLifecycles();
        preDestroyInterceptors = configuration.getPreDestroyLifecycles();
        // First, system interceptors (one of which should associate)
        final ArrayList<Interceptor> rootInterceptors = new ArrayList<Interceptor>();
        final SimpleInterceptorFactoryContext interceptorFactoryContext = new SimpleInterceptorFactoryContext();
        for (InterceptorFactory factory : configuration.getComponentSystemInterceptorFactories()) {
            rootInterceptors.add(factory.create(interceptorFactoryContext));
        }
        rootInterceptors.add(DISPATCHER);
        componentInterceptor = Interceptors.getChainedInterceptor(rootInterceptors);
        // Create the table of component class methods
        final Map<MethodIdentifier, Method> componentMethods = new HashMap<MethodIdentifier, Method>();
        final Map<Method, InterceptorFactory> componentToInterceptorFactory = new IdentityHashMap<Method, InterceptorFactory>();
        Class<?> currentClass = componentClass;
        do {
            final ClassReflectionIndex classIndex = index.getClassIndex(currentClass);
            // Mapping of method identifiers to component (target) methods
            // Mapping of component methods to corresponding instance interceptor factories
            for (Method method : classIndex.getMethods()) {
                int modifiers = method.getModifiers();
                if (! Modifier.isStatic(modifiers) && ! Modifier.isFinal(modifiers)) {
                    componentMethods.put(MethodIdentifier.getIdentifierForMethod(method), method);
                    // assemble the final set of interceptor factories for this method.
                    final List<InterceptorFactory> finalFactories = new ArrayList<InterceptorFactory>();
                    // TODO: default-level interceptors if applicable
                    // TODO: class-level interceptors if applicable
                    // TODO: method-level interceptors if applicable
                    // The final interceptor invokes the method on the associated instance
                    finalFactories.add(new MethodInvokingInterceptorFactory(INSTANCE_FACTORY, method));
                    componentToInterceptorFactory.put(method, Interceptors.getChainedInterceptorFactory(finalFactories));
                }
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);
        // Now create the views
        final Map<Class<?>, InvocationHandler> views = new HashMap<Class<?>, InvocationHandler>();
        // Mapping of view methods to corresponding instance interceptor factories
        final Map<Method, InterceptorFactory> viewToInterceptorFactory = new IdentityHashMap<Method, InterceptorFactory>();
        for (String viewClassName : configuration.getViewClassNames()) {
            final Class<?> viewClass;
            try {
                viewClass = Class.forName(viewClassName, false, deploymentClassLoader);
            } catch (ClassNotFoundException e) {
                NoClassDefFoundError error = new NoClassDefFoundError(e.getMessage());
                error.setStackTrace(e.getStackTrace());
                throw error;
            }
            ProxyFactory<?> factory = getProxyFactory(viewClass);
            // acquire the complete list of possible invoked methods for this view
            final List<Method> methods = new ArrayList<Method>();
            for (Method viewMethod : factory.getCachedMethods()) {
                methods.add(viewMethod);
                Method componentMethod = componentMethods.get(MethodIdentifier.getIdentifierForMethod(viewMethod));
                // todo - it's probably an error if the view has more methods than the component
                if (componentMethod != null) {
                    // Create the mapping of this view method to the interceptor factory for the target method
                    viewToInterceptorFactory.put(viewMethod, componentToInterceptorFactory.get(componentMethod));
                }
            }
            views.put(viewClass, new InvocationHandler(viewClass, factory, Collections.unmodifiableCollection(methods)));
        }
        this.views = views;
        interceptorFactoryMap = viewToInterceptorFactory;
    }

    private static <T> ProxyFactory<?> getProxyFactory(Class<T> type) {
        // TODO: Create this proxy into the right class loader.
        if (type.isInterface()) {
            return new ProxyFactory<Object>(Object.class, type);
        } else {
            return new ProxyFactory<T>(type);
        }
    }

    /** {@inheritDoc} */
    public ComponentInstance createInstance() {
        NamespaceContextSelector.pushCurrentSelector(selector);
        try {
            Object objectInstance = createObjectInstance();
            applyInjections(objectInstance);
            performPostConstructLifecycle(objectInstance);
            return constructComponentInstance(objectInstance);
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
            return componentClass.newInstance();
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
     * Construct the component instance.  After the instance is constructed, injections and lifecycle methods will
     * be called upon it.
     *
     * @param instance the object instance to wrap
     * @return the component instance
     */
    protected abstract AbstractComponentInstance constructComponentInstance(Object instance);

    /**
     * Get the class of this bean component.
     *
     * @return the class
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    /**
     * Apply the injections to a newly retrieved bean instance.
     *
     * @param instance The bean instance
     */
    protected void applyInjections(final Object instance) {
        final List<ResourceInjection> resourceInjections = this.resourceInjections;
        if(resourceInjections != null) {
            for (ResourceInjection resourceInjection : resourceInjections) {
                resourceInjection.inject(instance);
            }
        }
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
            setContextClassLoader(componentClass.getClassLoader());
            try {
                for (ComponentLifecycle postConstructMethod : postConstructMethods) {
                    try {
                        postConstructMethod.invoke(instance);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method for class " + getComponentClass(), t);
                    }
                }
            } finally {
                setContextClassLoader(contextCl);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroyInstance(final ComponentInstance instance) {
        performPreDestroyLifecycle(instance);
        applyUninjections(instance);
    }

    /** {@inheritDoc} */
    public ComponentInvocationHandler createClient(final Class<?> viewClass) {
        final InvocationHandler handler = views.get(viewClass);
        if (handler == null) {
            throw new IllegalArgumentException("No view for " + viewClass);
        }
        return handler;
    }

    /**
     * Create a new client-side interceptor for a local proxy.  A new interceptor is created for every client
     * instance.
     *
     * @param viewClass the view class in use
     * @return the client-side interceptor
     */
    protected abstract Interceptor createClientInterceptor(final Class<?> viewClass);

    public <T> T createLocalProxy(final Class<T> viewClass) {
        final Interceptor clientInterceptor = createClientInterceptor(viewClass);
        final InterceptorInvocationHandler handler = new InterceptorInvocationHandler(Interceptors.getChainedInterceptor(clientInterceptor, componentInterceptor));
        try {
            return viewClass.cast(views.get(viewClass).proxyFactory.newInstance(handler));
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public NamespaceContextSelector getNamespaceContextSelector() {
        return selector;
    }

    /**
     * Remove all injections.
     *
     * @param instance the instance to uninject
     */
    protected void applyUninjections(final Object instance) {
        for (ResourceInjection resourceInjection : resourceInjections) {
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
        setContextClassLoader(componentClass.getClassLoader());
        try {
            for (ComponentLifecycle preDestroyMethod : preDestroyInterceptors) {
                try {
                    preDestroyMethod.invoke(instance);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to invoke pre destroy method for class " + getComponentClass(), t);
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

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
    }

    /**
     * Set the application naming context.
     *
     * @param applicationContext the application naming context
     */
    public void setApplicationContext(javax.naming.Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Set the module naming context.
     *
     * @param moduleContext the module naming context
     */
    public void setModuleContext(javax.naming.Context moduleContext) {
        this.moduleContext = moduleContext;
    }

    /**
     * Set the component naming context.
     *
     * @param componentContext the component naming context
     */
    public void setComponentContext(javax.naming.Context componentContext) {
        this.componentContext = componentContext;
    }

    /** {@inheritDoc} */
    public Object createRemoteProxy(final Class<?> view, final ClassLoader targetClassLoader) {
        throw new UnsupportedOperationException("One thing at a time!");
    }

    class InvocationHandler implements ComponentInvocationHandler {
        private final Class<?> viewClass;
        private final ProxyFactory<?> proxyFactory;
        private final Collection<Method> allowedMethods;

        InvocationHandler(final Class<?> viewClass, final ProxyFactory<?> proxyFactory, final Collection<Method> allowedMethods) {
            this.viewClass = viewClass;
            this.proxyFactory = proxyFactory;
            this.allowedMethods = allowedMethods;
        }

        /** {@inheritDoc} */
        public Component getComponent() {
            return AbstractComponent.this;
        }

        /** {@inheritDoc} */
        public Class<?> getViewClass() {
            return viewClass;
        }

        /** {@inheritDoc} */
        public Collection<Method> allowedMethods() {
            return allowedMethods;
        }

        /** {@inheritDoc} */
        public void destroy() {
            // no op by default
        }

        /** {@inheritDoc} */
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            InterceptorContext context = new InterceptorContext();
            context.setMethod(method);
            context.setParameters(args);
            context.setTarget(proxy);
            return componentInterceptor.processInvocation(context);
        }
    }
}
