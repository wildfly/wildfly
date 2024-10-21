/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.context;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import jakarta.ejb.EJBLocalObject;
import jakarta.ejb.EJBObject;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TimerService;
import jakarta.transaction.UserTransaction;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
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

        // rls debug  todo remove once analysis is complete
        if (ROOT_LOGGER.isTraceEnabled()) {
            java.security.Principal p = instance.getComponent().getCallerPrincipal();
            if (p != null) {
                ROOT_LOGGER.trace("## SessionContextImpl  principal: " + p.getName());
                //new Throwable("## SessionContextImpl  principal: " + p.getName()
                //+ ",  this: " + this).printStackTrace();
            }
        }
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
