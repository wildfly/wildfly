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

package org.jboss.as.ejb3.security;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import javax.security.jacc.PolicyContext;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.security.AnybodyPrincipal;
import org.jboss.security.NobodyPrincipal;
import org.jboss.security.SimplePrincipal;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * EJB authorization interceptor responsible for handling invocation on EJB methods and doing the necessary authorization
 * checks on the invoked method.
 * <p/>
 * User: Jaikiran Pai
 */
public class AuthorizationInterceptor implements Interceptor {

    /**
     * EJB method security metadata
     */
    private final EJBMethodSecurityAttribute ejbMethodSecurityMetaData;

    /**
     * The view class name to which this interceptor is applicable
     */
    private final String viewClassName;

    /**
     * The view method to which this interceptor is applicable
     */
    private final Method viewMethod;

    /*
     * The JACC contextID to be used by this interceptor.
     */
    private final String contextID;

    public AuthorizationInterceptor(final EJBMethodSecurityAttribute ejbMethodSecurityMetaData, final String viewClassName, final Method viewMethod, final String contextID) {
        if (ejbMethodSecurityMetaData == null) {
            throw EjbLogger.ROOT_LOGGER.ejbMethodSecurityMetaDataIsNull();
        }
        if (viewClassName == null || viewClassName.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.viewClassNameIsNull();
        }
        if (viewMethod == null) {
            throw EjbLogger.ROOT_LOGGER.viewMethodIsNull();
        }
        this.ejbMethodSecurityMetaData = ejbMethodSecurityMetaData;
        this.viewClassName = viewClassName;
        this.viewMethod = viewMethod;
        this.contextID = contextID;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }
        final Method invokedMethod = context.getMethod();
        final ComponentView componentView = context.getPrivateData(ComponentView.class);
        final String viewClassOfInvokedMethod = componentView.getViewClass().getName();
        // shouldn't really happen if the interceptor was setup correctly. But let's be safe and do a check
        if (!this.viewClassName.equals(viewClassOfInvokedMethod) || !this.viewMethod.equals(invokedMethod)) {
            throw EjbLogger.ROOT_LOGGER.failProcessInvocation(this.getClass().getName(), invokedMethod, viewClassOfInvokedMethod, viewMethod, viewClassName);
        }
        final EJBComponent ejbComponent = (EJBComponent) component;
        final ServerSecurityManager securityManager = ejbComponent.getSecurityManager();
        final MethodInterfaceType methodIntfType = this.getMethodInterfaceType(componentView.getPrivateData(MethodIntf.class));

        // set the JACC contextID before calling the security manager.
        final String previousContextID = setContextID(this.contextID);
        try {
            if(WildFlySecurityManager.isChecking()) {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public ProtectionDomain run() {

                            if (!securityManager.authorize(ejbComponent.getComponentName(), componentView.getProxyClass().getProtectionDomain().getCodeSource(),
                                    methodIntfType.name(), AuthorizationInterceptor.this.viewMethod, AuthorizationInterceptor.this.getMethodRolesAsPrincipals(), AuthorizationInterceptor.this.contextID)) {
                                throw EjbLogger.ROOT_LOGGER.invocationOfMethodNotAllowed(invokedMethod,ejbComponent.getComponentName());
                            }
                            return null;
                        }
                    });
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                }
            } else {
                if (!securityManager.authorize(ejbComponent.getComponentName(), componentView.getProxyClass().getProtectionDomain().getCodeSource(),
                        methodIntfType.name(), this.viewMethod, this.getMethodRolesAsPrincipals(), this.contextID)) {
                    throw EjbLogger.ROOT_LOGGER.invocationOfMethodNotAllowed(invokedMethod,ejbComponent.getComponentName());
                }
            }
            // successful authorization, let the invocation proceed
            return context.proceed();
        }
        finally {
            // reset the previous JACC contextID.
            setContextID(previousContextID);
        }
    }

    /**
     * <p>
     * Returns the method roles as a set of {@code Principal} instances. All roles specified in the method-permissions or
     * via {@code RolesAllowed} for this method are wrapped by a {@code SimplePrincipal}. If the method has been added to
     * the exclude-list or annotated with {@code DenyAll}, a NOBODY_PRINCIPAL is returned. If the method has been added
     * to the unchecked list or annotated with {@code PermitAll}, an ANYBODY_PRINCIPAL is returned.
     * </p>
     *
     * @return the constructed set of role principals.
     */
    protected Set<Principal> getMethodRolesAsPrincipals() {
        Set<Principal> methodRoles = new HashSet<Principal>();
        if (this.ejbMethodSecurityMetaData.isDenyAll())
            methodRoles.add(NobodyPrincipal.NOBODY_PRINCIPAL);
        else if (this.ejbMethodSecurityMetaData.isPermitAll())
            methodRoles.add(AnybodyPrincipal.ANYBODY_PRINCIPAL);
        else {
            for (String role : this.ejbMethodSecurityMetaData.getRolesAllowed())
                methodRoles.add(new SimplePrincipal(role));
        }
        return methodRoles;
    }

    /**
     * <p>
     * Gets the {@code MethodInterfaceType} that corresponds to the specified {@code MethodIntf}.
     * </p>
     *
     * @param viewType the {@code MethodIntf} type to be converted.
     * @return the converted type or {@code null} if the type cannot be converted.
     */
    protected MethodInterfaceType getMethodInterfaceType(MethodIntf viewType) {
        switch (viewType) {
            case HOME:
                return MethodInterfaceType.Home;
            case LOCAL_HOME:
                return MethodInterfaceType.LocalHome;
            case SERVICE_ENDPOINT:
                return MethodInterfaceType.ServiceEndpoint;
            case LOCAL:
                return MethodInterfaceType.Local;
            case REMOTE:
                return MethodInterfaceType.Remote;
            case TIMER:
                return MethodInterfaceType.Timer;
            case MESSAGE_ENDPOINT:
                return MethodInterfaceType.MessageEndpoint;
            default:
                return null;
        }
    }

    /**
     * <p>
     * Sets the JACC contextID using a privileged action and returns the previousID from the {@code PolicyContext}.
     * </p>
     *
     * @param contextID the JACC contextID to be set.
     * @return the previous contextID as retrieved from the {@code PolicyContext}.
     */
    protected String setContextID(final String contextID) {
        if (! WildFlySecurityManager.isChecking()) {
            final String previousID = PolicyContext.getContextID();
            PolicyContext.setContextID(contextID);
            return previousID;
        } else {
            final PrivilegedAction<String> action = new SetContextIDAction(contextID);
            return AccessController.doPrivileged(action);
        }
    }

    /**
     * PrivilegedAction that sets the {@code PolicyContext} id.
     */
    private static class SetContextIDAction implements PrivilegedAction<String> {

        private String contextID;

        SetContextIDAction(final String contextID) {
            this.contextID = contextID;
        }

        @Override
        public String run() {
            final String previousID = PolicyContext.getContextID();
            PolicyContext.setContextID(this.contextID);
            return previousID;
        }
    }
}
