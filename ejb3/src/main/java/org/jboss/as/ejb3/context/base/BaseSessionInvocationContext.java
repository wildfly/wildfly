/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.context.base;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import org.jboss.as.ee.component.interceptors.DependencyInjectionCompleteMarker;
import org.jboss.as.ejb3.context.spi.SessionBeanComponent;
import org.jboss.as.ejb3.context.spi.SessionContext;
import org.jboss.as.ejb3.context.spi.SessionInvocationContext;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class BaseSessionInvocationContext extends BaseInvocationContext
        implements SessionInvocationContext {
    private final Class<?> invokedBusinessInterface;

    private MessageContext messageContext;
    private Future future;

    public BaseSessionInvocationContext(Class<?> invokedBusinessInterface, Method method, Object[] parameters) {
        super(method, parameters);

        // might be null for non-EJB3 invocations & lifecycle callbacks
        this.invokedBusinessInterface = invokedBusinessInterface;
    }

    public BaseSessionInvocationContext(boolean lifecycleCallback, Class<?> invokedBusinessInterface, Method method, Object[] parameters) {
        super(lifecycleCallback, method, parameters);

        // might be null for non-EJB3 invocations & lifecycle callbacks
        this.invokedBusinessInterface = invokedBusinessInterface;
    }

    public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException {
        // we need an instance attached
        SessionContext ctx = getEJBContext();
        return ctx.getComponent().getBusinessObject(ctx, businessInterface);
    }

    public SessionContext getEJBContext() {
        return (SessionContext) super.getEJBContext();
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        SessionContext ctx = getEJBContext();
        return ctx.getComponent().getEJBLocalObject(ctx);
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        SessionContext ctx = getEJBContext();
        return ctx.getComponent().getEJBObject(ctx);
    }

    public Class<?> getInvokedBusinessInterface() throws IllegalStateException {
        if (invokedBusinessInterface == null)
            throw new IllegalStateException("No invoked business interface on " + this);
        if(EJBObject.class.isAssignableFrom(invokedBusinessInterface)) {
            throw new IllegalStateException("Cannot call getInvokedBusinessInterface when invoking through EJBObject");
        }
        if(EJBLocalObject.class.isAssignableFrom(invokedBusinessInterface)) {
            throw new IllegalStateException("Cannot call getInvokedBusinessInterface when invoking through EJBLocalObject");
        }
        return invokedBusinessInterface;
    }

    public SessionBeanComponent getComponent() {
        // for now
        return getEJBContext().getComponent();
    }

    public MessageContext getMessageContext() throws IllegalStateException {
        if (messageContext == null)
            throw new IllegalStateException("No message context on " + this);
        return messageContext;
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }

    public boolean wasCancelCalled() throws IllegalStateException {
        if (future == null)
            throw new IllegalStateException("No asynchronous invocation in progress");
        return future.isCancelled();
    }

    @Override
    public TimerService getTimerService() {
        if(lifecycleCallback && !DependencyInjectionCompleteMarker.isDependencyInjectionComplete(getContext())) {
            throw new IllegalStateException("getTimerService() is not allowed while dependency injection is in progress");
        }
        return super.getTimerService();
    }

    @Override
    public UserTransaction getUserTransaction() {
        if(lifecycleCallback && !DependencyInjectionCompleteMarker.isDependencyInjectionComplete(getContext())) {
            throw new IllegalStateException("getUserTransaction() is not allowed while dependency injection is in progress");
        }
        return super.getUserTransaction();
    }

    public abstract InterceptorContext getContext();
}
