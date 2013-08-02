/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.mgmt.access.util;

import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.SUPERUSER_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.addRoleMapping;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.addRoleUser;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.removeRoleMapping;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.removeRoleUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * {@link ServerSetupTask} that can add user->role mappings to the access=authorization configuration.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class UserRolesMappingServerSetupTask implements ServerSetupTask {

    private final Map<String, Set<String>> rolesToUsers = new HashMap<String, Set<String>>();

    protected UserRolesMappingServerSetupTask(final Map<String, Set<String>> rolesToUsers) {
        this.rolesToUsers.putAll(rolesToUsers);
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();
        for (Map.Entry<String, Set<String>> roleEntry : rolesToUsers.entrySet()) {
            String role = roleEntry.getKey();
            addRoleMapping(role, client);
            for (String user : roleEntry.getValue()) {
                addRoleUser(role, user, client);
            }
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();
        for (Map.Entry<String, Set<String>> roleEntry : rolesToUsers.entrySet()) {
            String role = roleEntry.getKey();
            for (String user : roleEntry.getValue()) {
                removeRoleUser(role, user, client);
            }
            if (!SUPERUSER_USER.equals(role)) {
                removeRoleMapping(role, client);
            }
        }
    }

    /**
     * {@link UserRolesMappingServerSetupTask} that adds a single user mapping for each standard
     * role, with the username the same as the role name.
     */
    public static class StandardUsersSetup extends UserRolesMappingServerSetupTask {

        private static final Map<String, Set<String>> STANDARD_USERS;

        static {
            Map<String, Set<String>> rolesToUsers = new HashMap<String, Set<String>>();
            rolesToUsers.put(MONITOR_USER, Collections.singleton(MONITOR_USER));
            rolesToUsers.put(OPERATOR_USER, Collections.singleton(OPERATOR_USER));
            rolesToUsers.put(MAINTAINER_USER, Collections.singleton(MAINTAINER_USER));
            rolesToUsers.put(DEPLOYER_USER, Collections.singleton(DEPLOYER_USER));
            rolesToUsers.put(ADMINISTRATOR_USER, Collections.singleton(ADMINISTRATOR_USER));
            rolesToUsers.put(AUDITOR_USER, Collections.singleton(AUDITOR_USER));
            rolesToUsers.put(SUPERUSER_USER, Collections.singleton(SUPERUSER_USER));
            STANDARD_USERS = rolesToUsers;
        }

        public StandardUsersSetup() {
            super(STANDARD_USERS);
        }
    }
}
