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
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.AuthorizationFailureException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RunAsPrincipalInterceptor implements Interceptor {
    public static final String ANONYMOUS_PRINCIPAL = "anonymous";
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
            if (runAsPrincipal.equals(ANONYMOUS_PRINCIPAL)) {
                try {
                    newIdentity = currentIdentity.createRunAsAnonymous();
                } catch (AuthorizationFailureException ex) {
                    newIdentity = currentIdentity.createRunAsAnonymous(false);
                }
            } else {
                if (! runAsPrincipalExists(securityDomain, runAsPrincipal)) {
                    newIdentity = securityDomain.createAdHocIdentity(runAsPrincipal);
                } else {
                    try {
                        newIdentity = currentIdentity.createRunAsIdentity(runAsPrincipal);
                    } catch (AuthorizationFailureException ex) {
                        newIdentity = currentIdentity.createRunAsIdentity(runAsPrincipal, false);
                    }
                }
            }
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

    private boolean runAsPrincipalExists(final SecurityDomain securityDomain, final String runAsPrincipal) throws RealmUnavailableException {
        RealmIdentity realmIdentity = null;
        try {
            realmIdentity = securityDomain.getIdentity(runAsPrincipal);
            return realmIdentity.exists();
        } finally {
            if (realmIdentity != null) {
                realmIdentity.dispose();
            }
        }
    }
}
