/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jberet.spi.BatchEnvironment;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * A batch environment which can provide various security options.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface SecurityAwareBatchEnvironment extends BatchEnvironment {

    /**
     * Returns the security domain if defined or {@code null} if not defined.
     *
     * @return the security domain or {@code null} if not defined
     */
    SecurityDomain getSecurityDomain();

    /**
     * If the {@linkplain #getSecurityDomain() security domain} is not {@code null} the current user is returned.
     * otherwise {@code null} is returned.
     * <p>
     * Note that if the current identity is anonymous {@code null} will be returned.
     * </p>
     *
     * @return the current user name or {@code null}
     */
    default String getCurrentUserName() {
        final SecurityIdentity securityIdentity = getIdentity();
        if (securityIdentity != null && !securityIdentity.isAnonymous()) {
            return securityIdentity.getPrincipal().getName();
        }
        return null;
    }

    /**
     * If the {@linkplain #getSecurityDomain() security domain} is not {@code null} the current identity is returned.
     * otherwise {@code null} is returned.
     *
     * @return the current identity or {@code null}
     */
    default SecurityIdentity getIdentity() {
        final SecurityDomain securityDomain = getSecurityDomain();
        if (securityDomain != null) {
            return securityDomain.getCurrentSecurityIdentity();
        }
        return null;
    }
}
