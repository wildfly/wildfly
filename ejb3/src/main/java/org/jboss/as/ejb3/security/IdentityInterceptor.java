/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.security.IdentityUtil.getCurrentSecurityDomain;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * An {@code Interceptor} that will always runAs the current {@code SecurityIdentity}
 * to activate any outflow identities.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public final class IdentityInterceptor implements Interceptor {

    public static final IdentityInterceptor INSTANCE = new IdentityInterceptor();

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final SecurityDomain securityDomain = getCurrentSecurityDomain();
        if (securityDomain != null) {
            SecurityIdentity identity = securityDomain.getCurrentSecurityIdentity();
            return identity.runAsSupplierEx(() -> context.proceed());
        } else {
            return context.proceed();
        }
    }

}
