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

package org.jboss.as.ee.component;


import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.InterceptorInstanceFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.service.StopContext;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A basic component implementation.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicComponent implements Component {

    public static final Object INSTANCE_KEY = new Object();
    public static final Object COMPONENT_INSTANCE_KEY = new Object();
    public static final Object INJECTION_HANDLE_KEY = new Object();

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
    private final InterceptorFactory postConstruct;
    private final InterceptorFactory preDestroy;
    private final ManagedReferenceFactory componentInstantiator;
    private final Map<Method, InterceptorFactory> interceptorFactoryMap;

    private volatile boolean gate;
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicInteger instanceCount = new AtomicInteger(1);
    private volatile StopContext stopContext;

    /**
     * Construct a new instance.
     *
     * @param createService the create service which created this component
     */
    public BasicComponent(final BasicComponentCreateService createService) {
        componentName = createService.getComponentName();
        componentClass = createService.getComponentClass();
        postConstruct = createService.getPostConstruct();
        preDestroy = createService.getPreDestroy();
        interceptorFactoryMap = createService.getComponentInterceptors();
        componentInstantiator = createService.getComponentInstantiator();
    }

    /**
     * {@inheritDoc}
     */
    public ComponentInstance createInstance() {
        if (!gate) {
            // Block until successful start
            synchronized (this) {
                if (stopping.get()) {
                    throw new IllegalStateException("Component is stopped");
                }
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
        BasicComponentInstance instance = constructComponentInstance();
        instanceCount.getAndIncrement();
        return instance;
    }

    /**
     * Construct the component instance.  Upon return, the object instance should have injections and lifecycle
     * invocations completed already.
     *
     * @return the component instance
     */
    protected final BasicComponentInstance constructComponentInstance() {
        // Interceptor factory context
        final SimpleInterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(Component.class, this);

        // Create the post-construct interceptors for the component instance
        final Interceptor componentInstancePostConstructInterceptor = this.getPostConstruct().create(context);
        // create the pre-destroy interceptors
        final Interceptor componentInstancePreDestroyInterceptor = this.getPreDestroy().create(context);

        final AtomicReference<ManagedReference> instanceReference = (AtomicReference<ManagedReference>) context.getContextData().get(BasicComponentInstance.INSTANCE_KEY);

        final Map<Method, InterceptorFactory> interceptorFactoryMap = this.getInterceptorFactoryMap();
        // This is an identity map.  This means that only <b>certain</b> {@code Method} objects will
        // match - specifically, they must equal the objects provided to the proxy.
        final IdentityHashMap<Method, Interceptor> interceptorMap = new IdentityHashMap<Method, Interceptor>();
        for (Method method : interceptorFactoryMap.keySet()) {
            interceptorMap.put(method, interceptorFactoryMap.get(method).create(context));
        }

        // create the component instance
        BasicComponentInstance basicComponentInstance = this.instantiateComponentInstance(instanceReference, componentInstancePreDestroyInterceptor, interceptorMap);

        // now invoke the postconstruct interceptors
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.putPrivateData(Component.class, this);
        interceptorContext.putPrivateData(ComponentInstance.class, basicComponentInstance);
        try {
            componentInstancePostConstructInterceptor.processInvocation(interceptorContext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct component instance", e);
        }
        // return the component instance
        return basicComponentInstance;
    }

    /**
     * Responsible for instantiating the {@link BasicComponentInstance}. This method is *not* responsible for
     * handling the post construct activities like injection and lifecycle invocation. That is handled by
     * {@link #constructComponentInstance()}.
     * <p/>
     *
     * @return
     */
    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        // create and return the component instance
        return new BasicComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

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
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
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
     *
     * @param stopContext
     */
    public void stop(final StopContext stopContext) {
        if (stopping.compareAndSet(false, true)) {
            stopContext.asynchronous();
            synchronized (this) {
                gate = false;
                //this.stopContext = stopContext;
            }
            //TODO: only run this if there is no instances
            //TODO: trigger destruction of all component instances
            //TODO: this has lots of potential for race conditions unless we are careful
            stopContext.complete();
        }
    }

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
    }

    InterceptorFactory getPostConstruct() {
        return postConstruct;
    }

    InterceptorFactory getPreDestroy() {
        return preDestroy;
    }

    ManagedReferenceFactory getComponentInstantiator() {
        return componentInstantiator;
    }

    void finishDestroy() {
        //otherwise the server will hang
        if (instanceCount.decrementAndGet() == 0) {
            if (stopContext != null)
                stopContext.complete();
        }
    }
}
