/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

public class WildFlySecurityManager implements ActiveMQSecurityManager {
    private String defaultUser = null;
    private String defaultPassword = null;

    public WildFlySecurityManager() {
        defaultUser = DefaultCredentials.getUsername();
        defaultPassword = DefaultCredentials.getPassword();
    }

    @Override
    public boolean validateUser(String username, String password) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        throw MessagingLogger.ROOT_LOGGER.legacySecurityUnsupported();
    }

    @Override
    public boolean validateUserAndRole(final String username, final String password, final Set<Role> roles, final CheckType checkType) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        throw MessagingLogger.ROOT_LOGGER.legacySecurityUnsupported();
    }
}
