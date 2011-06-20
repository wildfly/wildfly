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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.EJBAccessException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * EJB authorization interceptor responsible for handling invocation on EJB methods and doing the necessary authorization
 * checks on the invoked method.
 * <p/>
 * User: Jaikiran Pai
 */
public class AuthorizationInterceptor implements Interceptor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(AuthorizationInterceptor.class);

    /**
     * EJB method security metadata
     */
    private final EJBMethodSecurityMetaData ejbMethodSecurityMetaData;

    /**
     * The view class name to which this interceptor is applicable
     */
    private final String viewClassName;

    /**
     * The view method to which this interceptor is applicable
     */
    private final Method viewMethod;

    public AuthorizationInterceptor(final EJBMethodSecurityMetaData ejbMethodSecurityMetaData, final String viewClassName, final Method viewMethod) {
        if (ejbMethodSecurityMetaData == null) {
            throw new IllegalArgumentException("EJB method security metadata cannot be null");
        }
        if (viewClassName == null || viewClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("View classname cannot be null or empty");
        }
        if (viewMethod == null) {
            throw new IllegalArgumentException("View method cannot be null");
        }
        this.ejbMethodSecurityMetaData = ejbMethodSecurityMetaData;
        this.viewClassName = viewClassName;
        this.viewMethod = viewMethod;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw new IllegalStateException("Unexpected component type: " + component.getClass() + " expected: " + EJBComponent.class);
        }
        final Method invokedMethod = context.getMethod();
        final ComponentViewInstance componentViewInstance = context.getPrivateData(ComponentViewInstance.class);
        // TODO: FIXME: Really fix me soon. WS invocation don't pass ComponentViewInstance.
        // We could have used the declaring class of the invoked method as the view  class name but for
        // a no-interface view, it won't work if the method is from the base class of the bean implementation class
        if (componentViewInstance != null) {
            final String viewClassOfInvokedMethod = componentViewInstance.getViewClass().getName();
            // shouldn't really happen if the interceptor was setup correctly. But let's be safe and do a check
            if (!this.viewClassName.equals(viewClassOfInvokedMethod) || !this.viewMethod.equals(invokedMethod)) {
                throw new IllegalStateException(this.getClass().getName() + " cannot handle method "
                        + invokedMethod + " of view class " + viewClassOfInvokedMethod + ".Expected view " +
                        "method to be " + viewMethod + " on view class " + viewClassName);
            }
            final EJBComponent ejbComponent = (EJBComponent) component;
            // check @DenyAll/exclude-list
            if (ejbMethodSecurityMetaData.isAccessDenied()) {
                throw new EJBAccessException("Invocation on method: " + invokedMethod + " of bean: " + ejbComponent.getComponentName()
                        + " is not allowed");
            }
            // If @PermitAll isn't applicable for the method then check the allowed roles
            if (!ejbMethodSecurityMetaData.isPermitAll()) {
                // get allowed roles (if any) for this method invocation
                final Collection<String> allowedRoles = ejbMethodSecurityMetaData.getRolesAllowed();
                if (!allowedRoles.isEmpty()) {
                    // call the security API to do authorization check
                    final SimpleSecurityManager securityManager = ejbComponent.getSecurityManager();
                    if (!securityManager.isCallerInRole(allowedRoles.toArray(new String[allowedRoles.size()]))) {
                        throw new EJBAccessException("Invocation on method: " + invokedMethod + " of bean: " +
                                ejbComponent.getComponentName() + " is not allowed");
                    }
                }
            }
        }
        // successful authorization, let the invocation proceed
        return context.proceed();
    }

}
