/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq;

import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
