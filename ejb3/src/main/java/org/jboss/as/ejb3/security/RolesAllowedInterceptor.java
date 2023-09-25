/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Roles;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class RolesAllowedInterceptor implements Interceptor {
    private final Collection<String> rolesAllowed;

    RolesAllowedInterceptor(final Collection<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    static final RolesAllowedInterceptor DENY_ALL = new RolesAllowedInterceptor(Collections.emptyList());

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (! (component instanceof EJBComponent)) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }
        final Iterator<String> iterator = rolesAllowed.iterator();
        if (iterator.hasNext()) {
            final SecurityDomain securityDomain = context.getPrivateData(SecurityDomain.class);
            final SecurityIdentity identity = securityDomain.getCurrentSecurityIdentity();
            final Roles ejbRoles = identity.getRoles("ejb", true);
            do {
                final String role = iterator.next();
                if (ejbRoles.contains(role) || (role.equals("**") && !identity.isAnonymous())) {
                    return context.proceed();
                }
            } while (iterator.hasNext());
        }
        throw EjbLogger.ROOT_LOGGER.invocationOfMethodNotAllowed(context.getMethod(), ((EJBComponent) component).getComponentName());
    }
}
