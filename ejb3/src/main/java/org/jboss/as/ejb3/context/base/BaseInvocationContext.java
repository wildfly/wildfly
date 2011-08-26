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

import org.jboss.as.ejb3.context.spi.EJBComponent;
import org.jboss.as.ejb3.context.spi.EJBContext;
import org.jboss.as.ejb3.context.spi.InvocationContext;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.lang.reflect.Method;
import java.security.Identity;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class BaseInvocationContext implements InvocationContext {
    private final Map<String, Object> contextData = new HashMap<String, Object>();
    protected final boolean lifecycleCallback;
    private final Method method;
    private Object[] parameters;

    private EJBContext instanceContext;
    private Principal callerPrincipal;
    private Timer timer;

    public BaseInvocationContext(Method method, Object[] parameters) {
        this(method == null, method, parameters);
    }

    public BaseInvocationContext(boolean lifecycleCallback, Method method, Object[] parameters) {
        this.lifecycleCallback = lifecycleCallback;
        // might be null for lifecycle callbacks
        this.method = method;
        this.parameters = parameters;
    }

    // redundant
    @SuppressWarnings({"deprecation"})
    public Identity getCallerIdentity() {
        throw new UnsupportedOperationException("getCallerIdentity is deprecated");
    }

    public Principal getCallerPrincipal() {
        if (callerPrincipal == null)
            throw new IllegalStateException("No callerPrincipal set on " + this);
        return callerPrincipal;
    }

    public EJBComponent getComponent() {
        // for now
        return getEJBContext().getComponent();
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }

    public EJBContext getEJBContext() {
        if (instanceContext == null)
            throw new IllegalStateException("No instance associated with invocation " + this);
        return instanceContext;
    }

    // redundant
    public EJBHome getEJBHome() {
        return getComponent().getEJBHome();
    }

    // redundant
    public EJBLocalHome getEJBLocalHome() {
        return getComponent().getEJBLocalHome();
    }

    // redundant
    public Properties getEnvironment() {
        throw new UnsupportedOperationException("getEnvironment is deprecated");
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getParameters() {
        if (method == null)
            throw new IllegalStateException("Getting parameters is not allowed on lifecycle callbacks");
        return parameters;
    }

    public boolean getRollbackOnly() {
        notAllowedInLifecycleCallbacks("getRollbackOnly");
        return getComponent().getRollbackOnly();
    }

    public Object getTarget() {
        return getEJBContext().getTarget();
    }

    public Object getTimer() {
        return timer;
    }

    // redundant
    public TimerService getTimerService() {
        return getComponent().getTimerService();
    }

    public UserTransaction getUserTransaction() {
        return getComponent().getUserTransaction();
    }

    // redundant
    @SuppressWarnings({"deprecation"})
    public boolean isCallerInRole(Identity role) {
        throw new IllegalStateException("deprecated");
    }

    public boolean isCallerInRole(String roleName) {
        // TODO: really?
        return getComponent().isCallerInRole(getCallerPrincipal(), roleName);
    }

    // redundant
    public Object lookup(String name) throws IllegalArgumentException {
        return getComponent().lookup(name);
    }

    protected final void notAllowedInLifecycleCallbacks(final String name) {
        if (lifecycleCallback)
            throw new IllegalStateException(name + " is not allowed in lifecycle callbacks (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)");
    }

    public abstract Object proceed() throws Exception;

    public void setCallerPrincipal(Principal callerPrincipal) {
        // FIXME: security check
        this.callerPrincipal = callerPrincipal;
    }

    public void setEJBContext(EJBContext instanceContext) {
        this.instanceContext = instanceContext;
    }

    public void setParameters(Object[] params) throws IllegalArgumentException, IllegalStateException {
        if (method == null)
            throw new IllegalStateException("Setting parameters is not allowed on lifecycle callbacks");
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (params != null) {
            if (parameterTypes.length != params.length)
                throw new IllegalArgumentException("Got wrong number of arguments, expected " + parameterTypes.length + ", got " + params.length + " on " + method);
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> expectedType = Primitives.normalize(parameterTypes[i]);
                Object param = params[i];
                if (param != null) {
                    Class<?> actualType = param.getClass();
                    if (!expectedType.isAssignableFrom(actualType))
                        throw new IllegalArgumentException("Parameter " + i + " has the wrong type, expected " + expectedType + ", got " + actualType + " on " + method);
                }
            }
        } else {
            if (parameterTypes.length != 0)
                throw new IllegalArgumentException("Got wrong number of arguments, expected " + parameterTypes.length + ", got none on " + method);
        }
        this.parameters = params;
    }

    public void setRollbackOnly() {
        notAllowedInLifecycleCallbacks("setRollbackOnly");
        getComponent().setRollbackOnly();
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
}