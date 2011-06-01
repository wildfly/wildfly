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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.cache.Identifiable;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.util.id.GUID;

import javax.ejb.EJBException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponentInstance extends SessionBeanComponentInstance implements Identifiable {
    private final GUID id;

    private final Interceptor afterBegin;
    private final Interceptor afterCompletion;
    private final Interceptor beforeCompletion;

    private boolean isDiscarded = false;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected StatefulSessionComponentInstance(final StatefulSessionComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, instanceReference, preDestroyInterceptor, methodInterceptors);
        this.id = new GUID();

        this.afterBegin = component.createInterceptor(component.afterBegin);
        this.afterCompletion = component.createInterceptor(component.afterCompletion);
        this.beforeCompletion = component.createInterceptor(component.beforeCompletion);
    }

    protected void afterBegin() {
        execute(afterBegin);
    }

    protected void afterCompletion(boolean committed) {
        execute(afterCompletion, committed);
    }

    protected void beforeCompletion() {
        execute(beforeCompletion);
    }

    protected void discard() {
        if (!isDiscarded) {
            isDiscarded = true;
            getComponent().getCache().discard(id);
        }
    }

    protected Object execute(final Interceptor interceptor, final Object... parameters) {
        if (interceptor == null)
            return null;
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.putPrivateData(Component.class, getComponent());
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        interceptorContext.putPrivateData(InvokeMethodOnTargetInterceptor.PARAMETERS_KEY, parameters);
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

    @Override
    public StatefulSessionComponent getComponent() {
        return (StatefulSessionComponent) super.getComponent();
    }

    public Serializable getId() {
        return id;
    }
}
