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


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.InterceptorInstanceFactory;

import java.lang.reflect.Method;
import java.util.Map;
import org.jboss.msc.service.StopContext;

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
     * Construct the component instance.  Upon return, the object instance should have injections and lifecycle
     * invocations completed already.
     *
     * @return the component instance
     */
    protected BasicComponentInstance constructComponentInstance() {
        return new BasicComponentInstance(this);
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
     * @param stopContext
     */
    public void stop(final StopContext stopContext) {
        if (stopping.compareAndSet(false, true)) {
            stopContext.asynchronous();
            synchronized (this) {
                gate = false;
                this.stopContext = stopContext;
            }
            finishDestroy();
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
        if (instanceCount.decrementAndGet() == 0) {
            stopContext.complete();
        }
    }
}
