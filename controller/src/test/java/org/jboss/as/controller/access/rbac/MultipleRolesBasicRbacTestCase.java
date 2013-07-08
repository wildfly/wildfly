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

package org.jboss.as.controller.access.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

import org.jboss.dmr.ModelNode;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class MultipleRolesBasicRbacTestCase extends BasicRbacTestCase {
    @Override
    protected ModelNode executeWithRole(ModelNode operation, StandardRole role) {
        // usually, just add "monitor", as that won't widen the permissions
        // for the "monitor" role, adding "deployer" won't widen the perms either
        StandardRole additionalRole = role != StandardRole.MONITOR ? StandardRole.MONITOR : StandardRole.DEPLOYER;
        // but for the "deployer" role, there's no role that won't widen the perms
        // so in this case, don't add any other role
        if (role == StandardRole.DEPLOYER) {
            additionalRole = StandardRole.DEPLOYER;
        }

        operation.get(OPERATION_HEADERS, "roles").add(role.name()).add(additionalRole.name());
        return getController().execute(operation, null, null, null);
    }
}
