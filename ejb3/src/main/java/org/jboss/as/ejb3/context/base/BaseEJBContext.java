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

import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.context.spi.EJBComponent;
import org.jboss.as.ejb3.context.spi.EJBContext;
import org.jboss.as.ejb3.context.spi.InvocationContext;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class BaseEJBContext implements EJBContext {
    private final EJBComponent manager;
    private final Object instance;

    public BaseEJBContext(EJBComponent manager, Object instance) {
        this.manager = manager;
        this.instance = instance;
    }

    @SuppressWarnings({"deprecation"})
    public Identity getCallerIdentity() {
        throw new UnsupportedOperationException("getCallerIdentity is deprecated");
    }

    public Principal getCallerPrincipal() {
        // per invocation
        return getCurrentInvocationContext().getCallerPrincipal();
    }

    public Map<String, Object> getContextData() {
        return getCurrentInvocationContext().getContextData();
    }

    protected InvocationContext getCurrentInvocationContext() {
        InvocationContext current = CurrentInvocationContext.get(InvocationContext.class);
        assert current.getEJBContext() == this;
        return current;
    }

    public EJBHome getEJBHome() {
        return manager.getEJBHome();
    }

    public EJBLocalHome getEJBLocalHome() {
        return manager.getEJBLocalHome();
    }

    public Properties getEnvironment() {
        throw new UnsupportedOperationException("getCallerIdentity is deprecated");
    }

    public EJBComponent getComponent() {
        return manager;
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        return getCurrentInvocationContext().getRollbackOnly();
    }

    public Object getTarget() {
        return instance;
    }

    public TimerService getTimerService() throws IllegalStateException {
        // to allow override per invocation
        return getCurrentInvocationContext().getTimerService();
    }

    public UserTransaction getUserTransaction() throws IllegalStateException {
        // to allow override per invocation
        return getCurrentInvocationContext().getUserTransaction();
    }

    @SuppressWarnings({"deprecation"})
    public boolean isCallerInRole(Identity role) {
        throw new IllegalStateException("deprecated");
    }

    public boolean isCallerInRole(String roleName) {
        return getCurrentInvocationContext().isCallerInRole(roleName);
    }

    public Object lookup(String name) throws IllegalArgumentException {
        return getComponent().lookup(name);
    }

    public void setRollbackOnly() throws IllegalStateException {
        // to allow override per invocation
        getCurrentInvocationContext().setRollbackOnly();
    }
}
