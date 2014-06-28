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

package org.jboss.as.test.integration.management.rbac;

import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleGroup;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleMapping;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.removeRoleGroup;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.removeRoleMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * {@link org.wildfly.core.testrunner.ServerSetupTask} that can add group->role mappings to the access=authorization configuration.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class GroupRolesMappingServerSetupTask {
    private final Map<String, Set<String>> rolesToGroups = new HashMap<String, Set<String>>();

    protected GroupRolesMappingServerSetupTask(final Map<String, Set<String>> rolesToGroups) {
        this.rolesToGroups.putAll(rolesToGroups);
    }

    public void setup(ModelControllerClient client) throws IOException {
        for (Map.Entry<String, Set<String>> roleEntry : rolesToGroups.entrySet()) {
            String role = roleEntry.getKey();
            addRoleMapping(role, client);
            for (String entity : roleEntry.getValue()) {
                addRoleGroup(role, entity, client);
            }
        }
    }

    public void tearDown(ModelControllerClient client) throws IOException {
        for (Map.Entry<String, Set<String>> roleEntry : rolesToGroups.entrySet()) {
            String role = roleEntry.getKey();
            for (String entity : roleEntry.getValue()) {
                removeRoleGroup(role, entity, client);
            }
            if (!SUPERUSER_ROLE.equals(role)) {
                removeRoleMapping(role, client);
            }
        }
    }
}
