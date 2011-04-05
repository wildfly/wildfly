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


import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.InterceptorInstanceFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.ee.component.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.component.SecurityActions.setContextClassLoader;

/**
 * The parent of all component classes.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponent implements Component {

    public static final Object INSTANCE_KEY = new Object();
    public static final Object COMPONENT_INSTANCE_KEY = new Object();
    public static final Object INJECTION_HANDLE_KEY = new Object();

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * An interceptor instance factory which will get an instance attached to the interceptor factory
     * context.
     */
    public static final InterceptorInstanceFactory INSTANCE_FACTORY = new InterceptorInstanceFactory() {
        public Object createInstance(final InterceptorFactoryContext context) {
            return context.getContextData().get(INSTANCE_KEY);
        }
    };

    private final String componentName;
    private final Class<?> componentClass;
    private final List<ResourceInjection> resourceInjections;
    private final InterceptorFactory postConstruct;
    private final InterceptorFactory preDestroy;
    private final List<ComponentInjector> componentInjectors;
    private Interceptor componentInterceptor;
    private final Map<Method, InterceptorFactory> interceptorFactoryMap;
    private final InjectedValue<NamespaceContextSelector> namespaceContextSelectorInjector = new InjectedValue<NamespaceContextSelector>();
    private final Map<Class<?>, ServiceName> viewServices;
    private final Map<Class<?>, ComponentView> views = new HashMap<Class<?>, ComponentView>();
    @Deprecated
    private final Collection<Method> componentMethods;

    private volatile boolean gate;

    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     */
    protected AbstractComponent(final AbstractComponentConfiguration configuration) {
        componentName = configuration.getComponentName();
        componentClass = configuration.getComponentClass();
        resourceInjections = configuration.getResourceInjections();
        interceptorFactoryMap = configuration.getInterceptorFactoryMap();
        this.componentInjectors = configuration.getComponentInjectors();
        this.viewServices = new HashMap<Class<?>, ServiceName>(configuration.getViewServices());
        this.componentMethods = configuration.getComponentMethods();


        //get the lifecycle interceptor chains
        postConstruct = Interceptors.getChainedInterceptorFactory(configuration.getPostConstruct());
        preDestroy = Interceptors.getChainedInterceptorFactory(configuration.getPreDestroy());

    }

    /**
     * {@inheritDoc}
     */
    public ComponentInstance createInstance() {
        if (!gate) {
            // Block until successful start
            synchronized (this) {
                while (!gate) {
                    // TODO: check for failure condition
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Component not available (interrupted)");
                    }
                }
            }
        }
        //we must use the same context over the life of the instance
        SimpleInterceptorFactoryContext interceptorContext = new SimpleInterceptorFactoryContext();

        Object objectInstance = createObjectInstance();


        //apply injections, and add the clean up interceptors to the pre destroy chain
        //we want interceptors that clean up injections to be last in the interceptor chain
        //so the injections are not cleaned up until all @AroundInvoke methods have been run

        AbstractComponentInstance instance = constructComponentInstance(objectInstance, interceptorContext);


        final List<ComponentInjector.InjectionHandle> injectionHandles = applyInjections(instance);
        interceptorContext.getContextData().put(AbstractComponent.INJECTION_HANDLE_KEY, injectionHandles);

        interceptorContext.getContextData().put(AbstractComponent.INSTANCE_KEY, objectInstance);
        interceptorContext.getContextData().put(AbstractComponent.COMPONENT_INSTANCE_KEY, instance);

        performLifecycle(instance, postConstruct, interceptorContext);

        // process the interceptors bound to individual methods
        // the interceptors are tied to the lifecycle of the instance
        final Map<Method, InterceptorFactory> factoryMap = getInterceptorFactoryMap();
        final Map<Method, Interceptor> methodMap = new IdentityHashMap<Method, Interceptor>(factoryMap.size());
        for (Map.Entry<Method, InterceptorFactory> entry : factoryMap.entrySet()) {
            Method method = entry.getKey();
            PerViewMethodInterceptorFactory.populate(interceptorContext, this, instance, method);
            InterceptorFactory interceptorFactory = entry.getValue();
            assert interceptorFactory != null : "Can't find interceptor factory for " + method;
            methodMap.put(method, interceptorFactory.create(interceptorContext));
        }
        instance.setMethodMap(methodMap);
        return instance;
    }

    /**
     * Create a new component object instance.  After the instance is constructed, injections and lifecycle methods will
     * be called upon it.
     *
     * @return the new instance
     */
    protected Object createObjectInstance() {
        try {
            Object instance = componentClass.newInstance();
            return instance;
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

    @Override
    public void destroyInstance(ComponentInstance instance) {
        performLifecycle(instance,preDestroy,instance.getInterceptorFactoryContext());
        final List<ComponentInjector.InjectionHandle> injectionHandles = (List<ComponentInjector.InjectionHandle>) instance.getInterceptorFactoryContext().getContextData().get(INJECTION_HANDLE_KEY);
        if(injectionHandles != null) {
            for(ComponentInjector.InjectionHandle handle : injectionHandles) {
                handle.uninject();
            }
        }
    }

    /**
     * Construct the component instance.  The object instance will have injections and lifecycle invocations completed
     * already.
     *
     *
     * @param instance               the object instance to wrap
     * @return the component instance
     */
    protected abstract AbstractComponentInstance constructComponentInstance(Object instance, InterceptorFactoryContext context);

    /**
     * Get the class of this bean component.
     *
     * @return the class
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    /**
     * Get the name of this bean component.
     *
     * @return
     */
    public String getComponentName() {
        return this.componentName;
    }

    /**
     * Apply the injections to a newly retrieved bean instance.
     *
     *
     *
     * @param componentInstance The bean instance
     * @return A list of interceptors that perform any required cleanup of injected objects when the component's lifecycle ends
     */
    protected List<ComponentInjector.InjectionHandle> applyInjections(final ComponentInstance componentInstance) {
        final List<ResourceInjection> resourceInjections = this.resourceInjections;
        if (resourceInjections != null) {
            for (ResourceInjection resourceInjection : resourceInjections) {
                resourceInjection.inject(componentInstance.getInstance());
            }
        }
        List<ComponentInjector.InjectionHandle> injectionHandles = new ArrayList<ComponentInjector.InjectionHandle>();
        for (ComponentInjector injector : componentInjectors) {
            injectionHandles.add(injector.inject(componentInstance.getInstance()));
        }
        return injectionHandles;
    }



    private void performLifecycle(final ComponentInstance instance, final InterceptorFactory lifecycleInterceptors, final InterceptorFactoryContext factoryContext) {
        final ClassLoader contextCl = getContextClassLoader();
        setContextClassLoader(componentClass.getClassLoader());
        try {
            final Interceptor interceptor = lifecycleInterceptors.create(factoryContext);
            try {
                final InterceptorContext context = new InterceptorContext();
                //as we use LifecycleInterceptorFactory we do not need to set the method
                context.setTarget(instance);
                context.setContextData(new HashMap<String, Object>());
                context.setParameters(EMPTY_OBJECT_ARRAY);
                interceptor.processInvocation(context);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to invoke post construct method for class " + getComponentClass(), t);
            }
        } finally {
            setContextClassLoader(contextCl);
        }
    }


    public List<ResourceInjection> getResourceInjections() {
        return Collections.unmodifiableList(resourceInjections);
    }

    /**
     * {@inheritDoc}
     */
    public ComponentEntry createClient(final Class<?> viewClass) {
        final ComponentView view = views.get(viewClass);
        if (view == null) {
            throw new IllegalArgumentException("Non-existent view " + viewClass + " requested");
        }
        final ManagedReference managedReference = view.getReference();
        final Method[] methods = view.getProxyFactory().getCachedMethods();
        final IdentityHashMap<Method, Interceptor> interceptorMap = new IdentityHashMap<Method, Interceptor>();
        final SimpleInterceptorFactoryContext interceptorFactoryContext = new SimpleInterceptorFactoryContext();
        for (Method method : methods) {
            final InterceptorFactory interceptorFactory = interceptorFactoryMap.get(method);
            if (interceptorFactory != null) {
                interceptorMap.put(method, interceptorFactory.create(interceptorFactoryContext));
            }
        }
        final Set<Method> allowedMethods = Collections.unmodifiableSet(interceptorFactoryMap.keySet());
        return new ComponentEntry() {
            public Component getComponent() {
                return AbstractComponent.this;
            }

            public Class<?> getViewClass() {
                return viewClass;
            }

            public Collection<Method> allowedMethods() {
                return allowedMethods;
            }

            public Interceptor getEntryPoint(final Method method) throws IllegalArgumentException {
                Interceptor interceptor = interceptorMap.get(method);
                if (interceptor == null) {
                    throw new IllegalArgumentException("No entry point found for " + method);
                }
                return interceptor;
            }

            public boolean isAsynchronous(final Method method) throws IllegalArgumentException {
                if (! interceptorMap.containsKey(method)) {
                    throw new IllegalArgumentException("No entry point found for " + method);
                }
                return false;
            }

            public void destroy() {
                managedReference.release();
            }
        };
    }

    public NamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelectorInjector.getValue();
    }

    // TODO: Jaikiran - Temporary to avoid compilation errors
    InjectedValue<NamespaceContextSelector> getNamespaceContextSelectorInjector() {
        return namespaceContextSelectorInjector;
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
        synchronized (this) {
            gate = true;
            notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        synchronized (this) {
            gate = false;
        }
    }

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object createRemoteProxy(final Class<?> view, final ClassLoader targetClassLoader, final Interceptor clientInterceptor) {
        throw new UnsupportedOperationException("One thing at a time!");
    }

    protected Interceptor getComponentInterceptor() {
        assert componentInterceptor != null : "componentInterceptor is null";
        return componentInterceptor;
    }

    void setComponentInterceptor(Interceptor interceptor) {
        this.componentInterceptor = interceptor;
    }

    void addComponentView(ComponentView view) {
        views.put(view.getViewClass(), view);
    }

    /**
     * Because interceptors are bound to a methods identity, you need the exact method
     * so find the interceptor. This should really be done during deployment via
     * reflection index and not during runtime operations.
     *
     * @param other     another method with the exact same signature
     * @return the method to which interceptors have been bound
     */
    @Deprecated
    public Method getComponentMethod(Method other) {
        for (Method id : componentMethods) {
            if (other.equals(id))
                return id;
        }
        throw new IllegalArgumentException("Can't find method " + other);
    }

    public ComponentView getComponentView(Class<?> viewClass) {
        return views.get(viewClass);
    }

    public Map<Class<?>, ServiceName> getViewServices() {
        return Collections.unmodifiableMap(viewServices);
    }

    void removeComponentView(ComponentView view) {
        views.remove(view.getViewClass());
    }
}
