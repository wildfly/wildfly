/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class IdentityUtil {

    public static SecurityDomain getCurrentSecurityDomain() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) SecurityDomain::getCurrent);
        } else {
            return SecurityDomain.getCurrent();
        }
    }
}
