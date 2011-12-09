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

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import java.lang.reflect.Method;
import java.util.Collection;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
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

    public AuthorizationInterceptor(final EJBMethodSecurityAttribute ejbMethodSecurityMetaData, final String viewClassName, final Method viewMethod) {
        if (ejbMethodSecurityMetaData == null) {
            throw MESSAGES.ejbMethodSecurityMetaDataIsNull();
        }
        if (viewClassName == null || viewClassName.trim().isEmpty()) {
            throw MESSAGES.viewClassNameIsNull();
        }
        if (viewMethod == null) {
            throw MESSAGES.viewMethodIsNull();
        }
        this.ejbMethodSecurityMetaData = ejbMethodSecurityMetaData;
        this.viewClassName = viewClassName;
        this.viewMethod = viewMethod;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw MESSAGES.unexpectedComponent(component,EJBComponent.class);
        }
        final Method invokedMethod = context.getMethod();
        final ComponentView componentView = context.getPrivateData(ComponentView.class);
        final String viewClassOfInvokedMethod = componentView.getViewClass().getName();
        // shouldn't really happen if the interceptor was setup correctly. But let's be safe and do a check
        if (!this.viewClassName.equals(viewClassOfInvokedMethod) || !this.viewMethod.equals(invokedMethod)) {
            throw MESSAGES.failProcessInvocation(this.getClass().getName(), invokedMethod,viewClassOfInvokedMethod, viewMethod, viewClassName);
        }
        final EJBComponent ejbComponent = (EJBComponent) component;
        // check @DenyAll/exclude-list
        if (ejbMethodSecurityMetaData.isDenyAll()) {
            throw MESSAGES.invocationOfMethodNotAllowed(invokedMethod,ejbComponent.getComponentName());
        }
        // If @PermitAll isn't applicable for the method then check the allowed roles
        if (!ejbMethodSecurityMetaData.isPermitAll()) {
            // get allowed roles (if any) for this method invocation
            final Collection<String> allowedRoles = ejbMethodSecurityMetaData.getRolesAllowed();
            if (!allowedRoles.isEmpty()) {
                // call the security API to do authorization check
                final SimpleSecurityManager securityManager = ejbComponent.getSecurityManager();
                final EJBSecurityMetaData ejbSecurityMetaData = ejbComponent.getSecurityMetaData();
                if (!securityManager.isCallerInRole(ejbSecurityMetaData.getSecurityRoles(), ejbSecurityMetaData.getSecurityRoleLinks(), allowedRoles.toArray(new String[allowedRoles.size()]))) {
                    throw MESSAGES.invocationOfMethodNotAllowed(invokedMethod,ejbComponent.getComponentName());
                }
            }
        }
        // successful authorization, let the invocation proceed
        return context.proceed();
    }

}
