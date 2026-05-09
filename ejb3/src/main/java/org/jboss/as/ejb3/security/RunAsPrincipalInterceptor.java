/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.security.PrivilegedActionException;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.jakarta.authz.RunAsIdentityHelper;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RunAsPrincipalInterceptor implements Interceptor {
    public static final String ANONYMOUS_PRINCIPAL = "anonymous";
    private final String runAsPrincipal;
    private final String runAsRole;
    private final RunAsIdentityHelper runAsHelper;

    public RunAsPrincipalInterceptor(final String runAsPrincipal, final String runAsRole, final RunAsIdentityHelper runAsHelper) {
        this.runAsPrincipal = runAsPrincipal;
        this.runAsRole = runAsRole;
        this.runAsHelper = runAsHelper;
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
            // Use helper to load RunAs identity
            newIdentity = runAsHelper.loadRunAsIdentity(securityDomain, runAsPrincipal, runAsRole);
            ejbComponent.setIncomingRunAsIdentity(currentIdentity);
            return newIdentity.runAs(context);
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if(cause != null) {
                if(cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw new RuntimeException(e);
                }
            } else {
                throw e;
            }
        } finally {
            ejbComponent.setIncomingRunAsIdentity(oldIncomingRunAsIdentity);
        }
    }
}
