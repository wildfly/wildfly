/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.context;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.TimerService;
import jakarta.transaction.UserTransaction;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBContextImpl implements jakarta.ejb.EJBContext, Serializable {
    private final EjbComponentInstance instance;

    public EJBContextImpl(final EjbComponentInstance instance) {
        this.instance = instance;
    }

    public Principal getCallerPrincipal() {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_CALLER_PRINCIPLE);
        // per invocation
        return instance.getComponent().getCallerPrincipal();
    }

    public Map<String, Object> getContextData() {
        final InterceptorContext invocation = CurrentInvocationContext.get();
        return invocation.getContextData();
    }

    public EJBHome getEJBHome() {
        return getComponent().getEJBHome();
    }

    public EJBLocalHome getEJBLocalHome() {
        return instance.getComponent().getEJBLocalHome();
    }

    public EJBComponent getComponent() {
        return instance.getComponent();
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        final InterceptorContext context = CurrentInvocationContext.get();
        if (context.getMethod() == null) {
            throw EjbLogger.ROOT_LOGGER.lifecycleMethodNotAllowed("getRollbackOnly");
        }
        return instance.getComponent().getRollbackOnly();
    }

    public Object getTarget() {
        return instance.getInstance();
    }

    public TimerService getTimerService() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_TIMER_SERVICE);
        return  instance.getComponent().getTimerService();
    }


    public UserTransaction getUserTransaction() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_USER_TRANSACTION);
        return getComponent().getUserTransaction();
    }

    public boolean isCallerInRole(String roleName) {
        AllowedMethodsInformation.checkAllowed(MethodType.IS_CALLER_IN_ROLE);
        return instance.getComponent().isCallerInRole(roleName);
    }

    public Object lookup(String name) throws IllegalArgumentException {
        return getComponent().lookup(name);
    }

    public void setRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        final InterceptorContext context = CurrentInvocationContext.get();
        if (context.getMethod() == null) {
            throw EjbLogger.ROOT_LOGGER.lifecycleMethodNotAllowed("getRollbackOnly");
        }
        instance.getComponent().setRollbackOnly();
    }
}
