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
package org.jboss.as.ejb3.context;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.invocation.InterceptorContext;

/**
 * Implementation of the SessionContext interface.
 * <p/>
 *
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SessionContextImpl extends EJBContextImpl implements SessionContext {

    private static final long serialVersionUID = 1L;
    private final boolean stateful;

    public SessionContextImpl(SessionBeanComponentInstance instance) {
        super(instance);
        stateful = instance.getComponent() instanceof StatefulSessionComponent;
    }

    public <T> T getBusinessObject(Class<T> businessInterface) throws IllegalStateException {
        // to allow override per invocation
        final InterceptorContext invocation = CurrentInvocationContext.get();
        return getComponent().getBusinessObject(businessInterface, invocation);
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_EJB_LOCAL_OBJECT);
        // to allow override per invocation
        final InterceptorContext invocation = CurrentInvocationContext.get();
        return getComponent().getEJBLocalObject(invocation);
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_EJB_OBJECT);
        // to allow override per invocation
        final InterceptorContext invocation = CurrentInvocationContext.get();
        return getComponent().getEJBObject(invocation);
    }

    public Class<?> getInvokedBusinessInterface() throws IllegalStateException {
        final InterceptorContext invocation = CurrentInvocationContext.get();
        final ComponentView view = invocation.getPrivateData(ComponentView.class);
        if (view.getViewClass().equals(getComponent().getEjbObjectType()) || view.getViewClass().equals(getComponent().getEjbLocalObjectType())) {
            throw EjbLogger.ROOT_LOGGER.cannotCall("getInvokedBusinessInterface", "EjbObject", "EJBLocalObject");
        }
        return view.getViewClass();
    }

    public SessionBeanComponent getComponent() {
        return (SessionBeanComponent) super.getComponent();
    }

    public MessageContext getMessageContext() throws IllegalStateException {
        final InterceptorContext invocation = CurrentInvocationContext.get();
        final MessageContext context = invocation.getPrivateData(MessageContext.class);
        if (context == null) {
            throw EjbLogger.ROOT_LOGGER.cannotCall("getMessageContext()", "MessageContext");

        }
        return context;
    }

    public boolean wasCancelCalled() throws IllegalStateException {
        final InterceptorContext invocation = CurrentInvocationContext.get();
        final CancellationFlag flag = invocation.getPrivateData(CancellationFlag.class);
        if (flag == null) {
            throw EjbLogger.ROOT_LOGGER.noAsynchronousInvocationInProgress();
        }
        return flag.isCancelFlagSet();
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_TIMER_SERVICE);
        if (stateful) {
            throw EjbLogger.ROOT_LOGGER.notAllowedFromStatefulBeans("getTimerService()");
        }
        return super.getTimerService();
    }

    @Override
    public UserTransaction getUserTransaction() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_USER_TRANSACTION);
        return getComponent().getUserTransaction();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.SET_ROLLBACK_ONLY);
        super.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_ROLLBACK_ONLY);
        return super.getRollbackOnly();
    }
}
