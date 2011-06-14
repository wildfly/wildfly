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

package org.jboss.as.ejb3.component.security;

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
 * User: Jaikiran Pai
 */
public class AuthorizationInterceptor implements Interceptor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(AuthorizationInterceptor.class);

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw new IllegalStateException("Unexpected component type: " + component.getClass() + " expected: " + EJBComponent.class);
        }
        final ComponentViewInstance viewInstance = context.getPrivateData(ComponentViewInstance.class);
        final Method invokedMethod = context.getMethod();
        final EJBComponent ejbComponent = (EJBComponent) component;
        final EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();
        // TODO: FIXME: Currently WS view invocations don't pass the ComponentViewInstance
        if (viewInstance != null) {
            final String viewClassName = viewInstance.getViewClass().getName();
            // check @DenyAll/exclude-list
            if (securityMetaData.isMethodAccessDenied(viewClassName, invokedMethod)) {
                throw new EJBAccessException("Invocation on method: " + invokedMethod + " of bean: " + ejbComponent.getComponentName() + " is not allowed");
            }
            // get allowed roles (if any) for this method invocation
            final Collection<String> allowedRoles = securityMetaData.getAllowedRoles(viewClassName, invokedMethod);
            if (!allowedRoles.isEmpty()) {
                // call the picketbox API to do authorization check
                final SimpleSecurityManager securityManager = ejbComponent.getSecurityManager();
                // TODO - SecurityManager isCallerInRoles is not valid for this call.
                if (!securityManager._isCallerInRole(allowedRoles.toArray(new String[allowedRoles.size()]))) {
                    throw new EJBAccessException("Invocation on method: " + invokedMethod + " of bean: " +
                            ejbComponent.getComponentName() + " is not allowed because caller is *not* in any of the " +
                            "allowed roles: " + allowedRoles);
                }
            }

        }
        return context.proceed();
    }

}
