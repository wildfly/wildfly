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
package org.jboss.as.ejb3.context;

import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.invocation.InterceptorContext;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBContextImpl implements javax.ejb.EJBContext {
    private final EjbComponentInstance instance;

    public EJBContextImpl(final EjbComponentInstance instance) {
        this.instance = instance;
    }

    @SuppressWarnings({"deprecation"})
    public Identity getCallerIdentity() {
        throw MESSAGES.isDeprecated("getCallerIdentity");
    }

    public Principal getCallerPrincipal() {
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

    public Properties getEnvironment() {
        throw MESSAGES.isDeprecated("getCallerIdentity");
    }

    public EJBComponent getComponent() {
        return instance.getComponent();
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        final InterceptorContext context = CurrentInvocationContext.get();
        if (context.getMethod() == null) {
            throw MESSAGES.lifecycleMethodNotAllowed("getRollbackOnly");
        }
        return instance.getComponent().getRollbackOnly();
    }

    public Object getTarget() {
        return instance.getInstance();
    }

    public TimerService getTimerService() throws IllegalStateException {
        return  instance.getComponent().getTimerService();
    }


    public UserTransaction getUserTransaction() throws IllegalStateException {
        return getComponent().getUserTransaction();
    }

    @SuppressWarnings({"deprecation"})
    public boolean isCallerInRole(Identity role) {
        throw MESSAGES.isDeprecatedIllegalState("isCallerInRole");
    }

    public boolean isCallerInRole(String roleName) {
        return instance.getComponent().isCallerInRole(roleName);
    }

    public Object lookup(String name) throws IllegalArgumentException {
        return getComponent().lookup(name);
    }

    public void setRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        final InterceptorContext context = CurrentInvocationContext.get();
        if (context.getMethod() == null) {
            throw MESSAGES.lifecycleMethodNotAllowed("getRollbackOnly");
        }
        instance.getComponent().setRollbackOnly();
    }
}
