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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

import static org.jboss.as.ee.EeLogger.ROOT_LOGGER;
import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * An abstract base component instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicComponentInstance implements ComponentInstance {

    private static final long serialVersionUID = -8099216228976950066L;

    public static final Object INSTANCE_KEY = new Object();

    private transient BasicComponent component;
    private transient AtomicReference<ManagedReference> instanceReference;
    private transient Interceptor preDestroy;
    @SuppressWarnings("unused")
    private volatile int done;

    private static final AtomicIntegerFieldUpdater<BasicComponentInstance> doneUpdater = AtomicIntegerFieldUpdater.newUpdater(BasicComponentInstance.class, "done");

    private transient Map<Method, Interceptor> methodMap;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected BasicComponentInstance(final BasicComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        // Associated component
        this.component = component;
        this.instanceReference = instanceReference;
        this.preDestroy = preDestroyInterceptor;
        this.methodMap = Collections.unmodifiableMap(methodInterceptors);
    }

    /**
     * {@inheritDoc}
     */
    public Component getComponent() {
        return component;
    }

    /**
     * {@inheritDoc}
     */
    public Object getInstance() {
        final ManagedReference managedReference = this.instanceReference.get();
        return managedReference.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public Interceptor getInterceptor(final Method method) throws IllegalStateException {
        Interceptor interceptor = methodMap.get(method);
        if (interceptor == null) {
            throw MESSAGES.methodNotFound(method);
        }
        return interceptor;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Method> allowedMethods() {
        return methodMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        if (doneUpdater.compareAndSet(this, 0, 1)) try {
            final ManagedReference reference = instanceReference.get();
            if (reference != null) {
                final InterceptorContext interceptorContext = prepareInterceptorContext();
                interceptorContext.setTarget(reference.getInstance());
                preDestroy.processInvocation(interceptorContext);
            }
        } catch (Exception e) {
            ROOT_LOGGER.componentDestroyFailure(e, this);
        } finally {
            component.finishDestroy();
        }
    }

    protected InterceptorContext prepareInterceptorContext() {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.putPrivateData(Component.class, component);
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        interceptorContext.setContextData(new HashMap<String, Object>());
        return interceptorContext;
    }

    protected void finalize() {
        destroy();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(this.component.getCreateServiceName().getCanonicalName());
        out.writeObject(this.instanceReference.get().getInstance());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        ServiceName name = ServiceName.parse(in.readUTF());
        ServiceController<?> service = CurrentServiceContainer.getServiceContainer().getRequiredService(name);
        this.component = (BasicComponent) service.getValue();
        BasicComponentInstance basic = this.component.constructComponentInstance(new ValueManagedReference(new ImmediateValue<Object>(in.readObject())), false);
        this.instanceReference = basic.instanceReference;
        this.methodMap = basic.methodMap;
        this.preDestroy = basic.preDestroy;
    }
}
