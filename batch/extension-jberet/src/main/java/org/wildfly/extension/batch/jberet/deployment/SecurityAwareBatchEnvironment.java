/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
