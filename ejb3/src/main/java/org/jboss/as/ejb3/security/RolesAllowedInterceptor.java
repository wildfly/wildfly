/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
