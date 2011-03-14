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


import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.InterceptorInstanceFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jboss.as.ee.component.SecurityActions.getContextClassLoader;
import static org.jboss.as.ee.component.SecurityActions.setContextClassLoader;

/**
 * The parent of all component classes.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponent implements Component {

    static final Object INSTANCE_KEY = new Object();

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
    private final List<ComponentLifecycle> postConstructMethods;
    private final List<ComponentLifecycle> preDestroyMethods;
    private final List<LifecycleInterceptorFactory> postConstructInterceptorsMethods;
    private final List<LifecycleInterceptorFactory> preDestroyInterceptorsMethods;
    private final List<ComponentInjector> componentInjectors;
    private Interceptor componentInterceptor;
    private final Map<Method, InterceptorFactory> interceptorFactoryMap;
    private final InjectedValue<NamespaceContextSelector> namespaceContextSelectorInjector = new InjectedValue<NamespaceContextSelector>();

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
        postConstructMethods = configuration.getPostConstructLifecycles();
        preDestroyMethods = configuration.getPreDestroyLifecycles();
        postConstructInterceptorsMethods = configuration.getPostConstructInterceptorLifecycles();
        preDestroyInterceptorsMethods = configuration.getPreDestroyInterceptorLifecycles();
        interceptorFactoryMap = configuration.getInterceptorFactoryMap();
        this.componentInjectors = configuration.getComponentInjectors();
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

        List<Interceptor> preDestoryInterceptors = new ArrayList<Interceptor>();
        createPreDestroyMethods(interceptorContext, preDestoryInterceptors);

        //apply injections, and add the clean up interceptors to the pre destroy chain
        //we want interceptors that clean up injections to be last in the interceptor chain
        //so the injections are not cleaned up until all @AroundInvoke methods have been run
        preDestoryInterceptors.addAll(applyInjections(objectInstance));

        performPostConstructLifecycle(objectInstance, interceptorContext);
        return constructComponentInstance(objectInstance, preDestoryInterceptors, interceptorContext);
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

    /**
     * Construct the component instance.  The object instance will have injections and lifecycle invocations completed
     * already.
     *
     * @param instance               the object instance to wrap
     * @param preDestroyInterceptors the interceptors to run on pre-destroy
     * @return the component instance
     */
    protected abstract AbstractComponentInstance constructComponentInstance(Object instance, List<Interceptor> preDestroyInterceptors, InterceptorFactoryContext context);

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
     * @param instance The bean instance
     * @return A list of interceptors that perform any required cleanup of injected objects when the component's lifecycle ends
     */
    protected List<Interceptor> applyInjections(final Object instance) {
        final List<ResourceInjection> resourceInjections = this.resourceInjections;
        if (resourceInjections != null) {
            for (ResourceInjection resourceInjection : resourceInjections) {
                resourceInjection.inject(instance);
            }
        }
        List<ComponentInjector.InjectionHandle> injectionHandles = new ArrayList<ComponentInjector.InjectionHandle>();
        for (ComponentInjector injector : componentInjectors) {
            injectionHandles.add(injector.inject(instance));
        }
        return Collections.<Interceptor>singletonList(new DisinjectionInterceptor(injectionHandles));
    }

    /**
     * Perform any post-construct life-cycle routines.  By default this will run any post-construct methods.
     *
     * @param instance           The bean instance
     * @param interceptorContext
     */
    protected void performPostConstructLifecycle(final Object instance, InterceptorFactoryContext interceptorContext) {
        final List<LifecycleInterceptorFactory> postConstructInterceptorMethods = this.postConstructInterceptorsMethods;
        if ((postConstructInterceptorMethods != null && !postConstructInterceptorMethods.isEmpty()) ||
                postConstructMethods != null && !postConstructMethods.isEmpty()) {
            final ClassLoader contextCl = getContextClassLoader();
            setContextClassLoader(componentClass.getClassLoader());
            try {
                for (final LifecycleInterceptorFactory postConstructMethod : postConstructInterceptorMethods) {
                    try {
                        Interceptor interceptor = postConstructMethod.create(interceptorContext);

                        final InterceptorContext context = new InterceptorContext();
                        context.setTarget(instance);
                        context.setContextData(new HashMap<String, Object>());
                        context.setParameters(EMPTY_OBJECT_ARRAY);
                        interceptor.processInvocation(context);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method for class " + getComponentClass(), t);
                    }
                }

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
    }

    protected void createPreDestroyMethods(final InterceptorFactoryContext context, List<Interceptor> interceptors) {
        for (LifecycleInterceptorFactory method : preDestroyInterceptorsMethods) {
            interceptors.add(method.create(context));
        }
    }

    /**
     * Perform any pre-destroy life-cycle routines.  By default it will invoke all pre-destroy methods.
     *
     * @param instance The bean instance
     */
    protected void performPreDestroyLifecycle(final ComponentInstance instance) {
        Iterator<Interceptor> preDestroyInterceptors = instance.getPreDestroyInterceptors().iterator();
        if (preDestroyInterceptors.hasNext() ||
                (preDestroyMethods != null && !preDestroyMethods.isEmpty())) {
            final ClassLoader contextCl = getContextClassLoader();
            setContextClassLoader(componentClass.getClassLoader());
            try {
                while (preDestroyInterceptors.hasNext()) {
                    try {
                        final Interceptor interceptor = preDestroyInterceptors.next();
                        final InterceptorContext context = new InterceptorContext();
                        //as we use LifecycleInterceptorFactory we do not need to set the method
                        context.setTarget(instance);
                        context.setContextData(new HashMap<String, Object>());
                        context.setParameters(EMPTY_OBJECT_ARRAY);
                        interceptor.processInvocation(context);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke post construct method for class " + getComponentClass(), t);
                    }
                }

                // Execute the post construct life-cycle
                for (ComponentLifecycle preDestroyMethod : preDestroyMethods) {
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
    }

    public List<ResourceInjection> getResourceInjections() {
        return Collections.unmodifiableList(resourceInjections);
    }

    /**
     * {@inheritDoc}
     */
    public ComponentInvocationHandler createClient(final Class<?> viewClass) {
        // todo for remote inv.
        return null;
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

    Interceptor getComponentInterceptor() {
        assert componentInterceptor != null : "componentInterceptor is null";
        return componentInterceptor;
    }

    void setComponentInterceptor(Interceptor interceptor) {
        this.componentInterceptor = interceptor;
    }

    /**
     * Interceptor that cleans up injected resources
     */
    private static class DisinjectionInterceptor implements Interceptor {

        private final List<ComponentInjector.InjectionHandle> injections;

        public DisinjectionInterceptor(List<ComponentInjector.InjectionHandle> injections) {
            this.injections = injections;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            for (ComponentInjector.InjectionHandle injectionHandle : injections) {
                injectionHandle.uninject();
            }
            return null;
        }
    }
}
