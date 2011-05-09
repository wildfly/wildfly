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
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract base component instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicComponentInstance implements ComponentInstance {

    private static final long serialVersionUID = -8099216228976950066L;

    public static final Object INSTANCE_KEY = new Object();

    private final BasicComponent component;
    private final Object instance;
    private final Interceptor preDestroy;
    @SuppressWarnings("unused")
    private volatile int done;

    private static final AtomicIntegerFieldUpdater<BasicComponentInstance> doneUpdater = AtomicIntegerFieldUpdater.newUpdater(BasicComponentInstance.class, "done");

    /**
     * This is an identity map.  This means that only <b>certain</b> {@code Method} objects will
     * match - specifically, they must equal the objects provided to the proxy.
     */
    private final Map<Method, Interceptor> methodMap;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected BasicComponentInstance(final BasicComponent component) {
        // Associated component
        this.component = component;

        // Interceptor factory context
        final SimpleInterceptorFactoryContext context = new SimpleInterceptorFactoryContext();

        context.getContextData().put(Component.class, component);
        context.getContextData().put(ComponentInstance.class, this);

        // Call post-construct hooks
        final Interceptor postConstruct = component.getPostConstruct().create(context);
        preDestroy = component.getPreDestroy().create(context);
        @SuppressWarnings("unchecked")
        final AtomicReference<ManagedReference> referenceReference = (AtomicReference<ManagedReference>) context.getContextData().get(INSTANCE_KEY);

        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.putPrivateData(Component.class, component);

        //TODO: this is very nasty, as we are exposing a partially constructed instance to the interceptor chain
        // this removes all the java memory model guarentees that having final fields gives us
        // this needs to be done ATM as we need this to get the EjbContext
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        try {
            postConstruct.processInvocation(interceptorContext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct component instance", e);
        }
        final Map<Method,InterceptorFactory> interceptorFactoryMap = component.getInterceptorFactoryMap();
        final IdentityHashMap<Method, Interceptor> interceptorMap = new IdentityHashMap<Method, Interceptor>();
        for (Method method : interceptorFactoryMap.keySet()) {
            interceptorMap.put(method, interceptorFactoryMap.get(method).create(context));
        }
        methodMap = Collections.unmodifiableMap(interceptorMap);
        instance = referenceReference.get().getInstance();
    }


    /** {@inheritDoc} */
    public Component getComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public Object getInstance() {
        return instance;
    }

    /** {@inheritDoc} */
    public Interceptor getInterceptor(final Method method) throws IllegalStateException {
        Interceptor interceptor = methodMap.get(method);
        if (interceptor == null) {
            throw new IllegalStateException("Method does not exist " + method);
        }
        return interceptor;
    }

    /** {@inheritDoc} */
    public Collection<Method> allowedMethods() {
        return methodMap.keySet();
    }

    /** {@inheritDoc} */
    public void destroy() {
        if (doneUpdater.compareAndSet(this, 0, 1)) try {
            final InterceptorContext interceptorContext = new InterceptorContext();
            interceptorContext.putPrivateData(Component.class, component);
            interceptorContext.putPrivateData(ComponentInstance.class, this);
            preDestroy.processInvocation(interceptorContext);
        } catch (Exception e) {
            // todo log it
        } finally {
            component.finishDestroy();
        }
    }

    protected void finalize() {
        destroy();
    }
}
