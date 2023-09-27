/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.security.IdentityUtil.getCurrentSecurityDomain;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * An {@code EJBClientInterceptor} that will always runAs the current {@code SecurityIdentity}
 * to activate any outflow identities.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public final class IdentityEJBClientInterceptor implements EJBClientInterceptor {

    public static final IdentityEJBClientInterceptor INSTANCE = new IdentityEJBClientInterceptor();

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        final SecurityDomain securityDomain = getCurrentSecurityDomain();
        if (securityDomain != null) {
            SecurityIdentity identity = securityDomain.getCurrentSecurityIdentity();
            identity.runAsSupplierEx(() -> {
                context.sendRequest();
                return null;
            });
        } else {
            context.sendRequest();
        }
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        return context.getResult();
    }

}
