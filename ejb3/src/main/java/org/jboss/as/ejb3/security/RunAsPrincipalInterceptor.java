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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.AuthorizationFailureException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RunAsPrincipalInterceptor implements Interceptor {
    private final String runAsPrincipal;

    public RunAsPrincipalInterceptor(final String runAsPrincipal) {
        this.runAsPrincipal = runAsPrincipal;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }
        final EJBComponent ejbComponent = (EJBComponent) component;

        // Set the incomingRunAsIdentity before switching users
        final SecurityDomain securityDomain = context.getPrivateData(SecurityDomain.class);
        Assert.checkNotNullParam("securityDomain", securityDomain);
        final SecurityIdentity currentIdentity = securityDomain.getCurrentSecurityIdentity();
        final SecurityIdentity oldIncomingRunAsIdentity = ejbComponent.getIncomingRunAsIdentity();
        SecurityIdentity newIdentity;
        try {
            // The run-as-principal operation should succeed if the current identity is authorized to
            // run as a user with the given name or if the caller has sufficient permission
            try {
                newIdentity = currentIdentity.createRunAsIdentity(runAsPrincipal);
            } catch (AuthorizationFailureException ex) {
                newIdentity = currentIdentity.createRunAsIdentity(runAsPrincipal, false);
            }
            ejbComponent.setIncomingRunAsIdentity(currentIdentity);
            return newIdentity.runAs(context);
        } finally {
            ejbComponent.setIncomingRunAsIdentity(oldIncomingRunAsIdentity);
        }
    }
}
