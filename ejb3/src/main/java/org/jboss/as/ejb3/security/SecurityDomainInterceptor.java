/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * An interceptor which sets the security domain of the invocation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SecurityDomainInterceptor implements Interceptor {
    private final SecurityDomain securityDomain;

    SecurityDomainInterceptor(final SecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final SecurityDomain oldDomain = context.putPrivateData(SecurityDomain.class, securityDomain);
        try {
            return context.proceed();
        } finally {
            context.putPrivateData(SecurityDomain.class, oldDomain);
        }
    }
}
