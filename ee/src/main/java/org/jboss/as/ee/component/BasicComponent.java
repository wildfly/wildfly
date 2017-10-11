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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.service.ServiceName;

/**
 * A basic component implementation.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicComponent implements Component {

    private final String componentName;
    private final Class<?> componentClass;
    private final InterceptorFactory postConstruct;
    private final InterceptorFactory preDestroy;
    private final Map<Method, InterceptorFactory> interceptorFactoryMap;
    private final NamespaceContextSelector namespaceContextSelector;
    private final ServiceName createServiceName;

    private volatile boolean gate;
    private final AtomicBoolean stopping = new AtomicBoolean();


    private Interceptor postConstructInterceptor;
    private Interceptor preDestroyInterceptor;
    private Map<Method, Interceptor> interceptorInstanceMap;


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
        namespaceContextSelector = createService.getNamespaceContextSelector();
        createServiceName = createService.getServiceName();
    }

    /**
     * {@inheritDoc}
     */
    public ComponentInstance createInstance() {
        BasicComponentInstance instance = constructComponentInstance(null, true);
        return instance;
    }

    /**
     * Wraps an existing object instance in a ComponentInstance, and run the post construct interceptor chain on it.
     *
     * @param instance The instance to wrap
     * @return The new ComponentInstance
     */
    public ComponentInstance createInstance(Object instance) {
        BasicComponentInstance obj = constructComponentInstance(new ImmediateManagedReference(instance), true);
        obj.constructionFinished();
        return obj;
    }

    public void waitForComponentStart() {
        if (!gate) {
            EeLogger.ROOT_LOGGER.tracef("Waiting for component %s (%s)", componentName, componentClass);
            // Block until successful start
            synchronized (this) {
                if (stopping.get()) {
                    throw EeLogger.ROOT_LOGGER.componentIsStopped();
                }
                while (!gate) {
                    // TODO: check for failure condition
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw EeLogger.ROOT_LOGGER.componentNotAvailable();
                    }
                }
            }
            EeLogger.ROOT_LOGGER.tracef("Finished waiting for component %s (%s)", componentName, componentClass);
        }
    }
    /**
     * Construct the component instance.  Upon return, the object instance should have injections and lifecycle
     * invocations completed already.
     *
     *
     * @param instance An instance to be wrapped, or null if a new instance should be created
     * @return the component instance
     */
    protected BasicComponentInstance constructComponentInstance(ManagedReference instance, boolean invokePostConstruct) {
        return constructComponentInstance(instance, invokePostConstruct, Collections.emptyMap());
    }

    /**
     * Construct the component instance.  Upon return, the object instance should have injections and lifecycle
     * invocations completed already.
     *
     *
     * @param instance An instance to be wrapped, or null if a new instance should be created
     * @return the component instance
     */
    protected BasicComponentInstance constructComponentInstance(ManagedReference instance, boolean invokePostConstruct, final Map<Object, Object> context) {
        waitForComponentStart();
        // create the component instance
        final BasicComponentInstance basicComponentInstance = this.instantiateComponentInstance(preDestroyInterceptor, interceptorInstanceMap, context);
        if(instance != null) {
            basicComponentInstance.setInstanceData(BasicComponentInstance.INSTANCE_KEY, instance);
        }
        if (invokePostConstruct) {
            // now invoke the postconstruct interceptors
            final InterceptorContext interceptorContext = new InterceptorContext();
            interceptorContext.putPrivateData(Component.class, this);
            interceptorContext.putPrivateData(ComponentInstance.class, basicComponentInstance);
            interceptorContext.putPrivateData(InvocationType.class, InvocationType.POST_CONSTRUCT);
            interceptorContext.setContextData(new HashMap<String, Object>());

            try {
                postConstructInterceptor.processInvocation(interceptorContext);
            } catch (Exception e) {
                throw EeLogger.ROOT_LOGGER.componentConstructionFailure(e);
            }
        }
        componentInstanceCreated(basicComponentInstance);
        // return the component instance
        return basicComponentInstance;
    }

    /**
     * Method that can be overridden to perform setup on the instance after it has been created
     *
     * @param basicComponentInstance The component instance
     *
     */
    protected void componentInstanceCreated(final BasicComponentInstance basicComponentInstance) {

    }


    /**
     * Responsible for instantiating the {@link BasicComponentInstance}. This method is *not* responsible for
     * handling the post construct activities like injection and lifecycle invocation. That is handled by
     * {@link #constructComponentInstance(org.jboss.as.naming.ManagedReference, boolean)}.
     * <p/>
     *
     * @return the component instance
     */
    protected BasicComponentInstance instantiateComponentInstance(final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, Map<Object, Object> context) {
        // create and return the component instance
        return new BasicComponentInstance(this, preDestroyInterceptor, methodInterceptors);

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

    public ServiceName getCreateServiceName() {
        return createServiceName;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void start() {
        init();

        this.stopping.set(false);
        gate = true;
        notifyAll();
    }

    public synchronized void init() {
        final InterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(Component.class, this);
        createInterceptors(context);
    }

    protected void createInterceptors(InterceptorFactoryContext context) {
        // Create the post-construct interceptors for the ComponentInstance
        postConstructInterceptor = this.postConstruct.create(context);
        // create the pre-destroy interceptors
        preDestroyInterceptor = this.getPreDestroy().create(context);

        final Map<Method, InterceptorFactory> interceptorFactoryMap = this.getInterceptorFactoryMap();
        // This is an identity map.  This means that only <b>certain</b> {@code Method} objects will
        // match - specifically, they must equal the objects provided to the proxy.
        final IdentityHashMap<Method, Interceptor> interceptorMap = new IdentityHashMap<Method, Interceptor>();
        for (Method method : interceptorFactoryMap.keySet()) {
            interceptorMap.put(method, interceptorFactoryMap.get(method).create(context));
        }
        this.interceptorInstanceMap = interceptorMap;
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        if (stopping.compareAndSet(false, true)) {
            synchronized (this) {
                gate = false;
                this.interceptorInstanceMap = null;
                this.preDestroyInterceptor = null;
                this.postConstructInterceptor = null;
            }
            //TODO: only run this if there is no instances
            //TODO: trigger destruction of all component instances
            //TODO: this has lots of potential for race conditions unless we are careful
            //TODO: using stopContext.asynchronous() and then executing synchronously is pointless.
            // Use org.jboss.as.server.Services#addServerExecutorDependency to inject an executor to do this async
        }
    }

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
    }

    InterceptorFactory getPreDestroy() {
        return preDestroy;
    }

    void finishDestroy() {

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + componentName;
    }

    /**
     * @return The components namespace context selector, or null if it does not have one
     */
    @Override
    public NamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelector;
    }

    public static ServiceName serviceNameOf(final ServiceName deploymentUnitServiceName, final String componentName) {
        return deploymentUnitServiceName.append("component").append(componentName);
    }
}
