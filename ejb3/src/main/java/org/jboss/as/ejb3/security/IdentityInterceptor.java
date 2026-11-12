/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.security.IdentityUtil.getCurrentSecurityDomain;

import org.jboss.as.ejb3.logging.EjbLogger;
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
            if (EjbLogger.ROOT_LOGGER.isTraceEnabled()) {
                EjbLogger.ROOT_LOGGER.trace("IdentityInterceptor: running as identity from current security domain"
                        + " securityDomain=" + securityDomain
                        + " identity.principal=" + identity.getPrincipal().getName()
                        + " identity.anonymous=" + identity.isAnonymous()
                        + " classLoader=" + Thread.currentThread().getContextClassLoader());
            }
            return identity.runAsSupplierEx(() -> context.proceed());
        } else {
            return context.proceed();
        }
    }

}
