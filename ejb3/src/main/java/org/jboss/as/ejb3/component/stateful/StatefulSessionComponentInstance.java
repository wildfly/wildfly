/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBException;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponentInstance extends SessionBeanComponentInstance implements Cacheable<SessionID> {
    private static final long serialVersionUID = 3803978357389448971L;

    private final SessionID id;

    private final Interceptor afterBegin;
    private final Interceptor afterCompletion;
    private final Interceptor beforeCompletion;
    private final Interceptor prePassivate;
    private final Interceptor postActivate;
    private final Interceptor ejb2XRemoveInterceptor;
    private volatile Map<Object, Object> serializableInterceptors;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected StatefulSessionComponentInstance(final StatefulSessionComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext factoryContext) {
        super(component, instanceReference, preDestroyInterceptor, methodInterceptors);

        final SessionID existingSession = (SessionID) factoryContext.getContextData().get(SessionID.class);
        if (existingSession != null) {
            this.id = existingSession;
        } else {
            SessionID id = null;
            do {
                final UUID uuid = UUID.randomUUID();
                ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
                bb.putLong(uuid.getMostSignificantBits());
                bb.putLong(uuid.getLeastSignificantBits());
                id = SessionID.createSessionID(bb.array());
            } while (!component.getCache().hasAffinity(id));
            this.id = id;
        }
        this.afterBegin = component.createInterceptor(component.getAfterBegin(), factoryContext);
        this.afterCompletion = component.createInterceptor(component.getAfterCompletion(), factoryContext);
        this.beforeCompletion = component.createInterceptor(component.getBeforeCompletion(), factoryContext);
        this.prePassivate = component.createInterceptor(component.getPrePassivate(), factoryContext);
        this.postActivate = component.createInterceptor(component.getPostActivate(), factoryContext);
        this.ejb2XRemoveInterceptor = component.createInterceptor(component.getEjb2XRemoveMethod(), factoryContext);
    }


    protected void afterBegin() {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.AFTER_BEGIN);
        try {
            execute(afterBegin, getComponent().getAfterBeginMethod());
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    protected void afterCompletion(boolean committed) {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.AFTER_COMPLETION);
        try {
            execute(afterCompletion, getComponent().getAfterCompletionMethod(), committed);
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    protected void beforeCompletion() {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.BEFORE_COMPLETION);
        try {
            execute(beforeCompletion, getComponent().getBeforeCompletionMethod());
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    protected void prePassivate() {
        this.execute(prePassivate, null);
    }

    protected void postActivate() {
        this.execute(postActivate, null);
    }

    public void discard() {
        if (!isDiscarded()) {
            super.discard();
            getComponent().getCache().discard(id);
        }
    }

    private Object execute(final Interceptor interceptor, final Method method, final Object... parameters) {
        if (interceptor == null)
            return null;
        final InterceptorContext interceptorContext = new InterceptorContext();
        //we need the method so this does not count as a lifecycle invocation
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, getComponent());
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        interceptorContext.putPrivateData(InvokeMethodOnTargetInterceptor.PARAMETERS_KEY, parameters);
        interceptorContext.setContextData(new HashMap<String, Object>());
        try {
            return interceptor.processInvocation(interceptorContext);
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    public Map<Object, Object> getSerializableInterceptors() {
        return serializableInterceptors;
    }

    public void setSerializableInterceptors(final Map<Object, Object> serializableInterceptors) {
        this.serializableInterceptors = serializableInterceptors;
    }

    @Override
    public StatefulSessionComponent getComponent() {
        return (StatefulSessionComponent) super.getComponent();
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public SessionID getId() {
        return id;
    }

    public Interceptor getEjb2XRemoveInterceptor() {
        return ejb2XRemoveInterceptor;
    }

    @Override
    public String toString() {
        return " Instance of " + getComponent().getComponentName() + " {" + id + "}";
    }

    public Object writeReplace() throws ObjectStreamException {
        return new SerializedStatefulSessionComponent(getInstanceReference().get(), id, getComponent().getCreateServiceName().getCanonicalName(), serializableInterceptors);
    }
}
