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

package org.jboss.as.test.integration.mgmt.access;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link org.jboss.as.controller.access.CombinationPolicy#REJECTING}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RejectingCombinationPolicyTestCase extends AbstractRbacTestCase {

    private static final PathAddress AC_ADDRESS =
            PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT),
                    PathElement.pathElement(ModelDescriptionConstants.ACCESS, ModelDescriptionConstants.AUTHORIZATION));

    @Before
    public void setup() throws IOException {
        ModelNode op = Util.getWriteAttributeOperation(AC_ADDRESS,
                ModelDescriptionConstants.PERMISSION_COMBINATION_POLICY,
                new ModelNode(CombinationPolicy.REJECTING.toString()));
        RbacUtil.executeOperation(getManagementClient().getControllerClient(), op, Outcome.SUCCESS);
    }

    @After
    public void tearDown() throws IOException {
        ModelNode op = Util.createEmptyOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION,
                AC_ADDRESS);
        op.get(ModelDescriptionConstants.NAME).set(ModelDescriptionConstants.PERMISSION_COMBINATION_POLICY);
        RbacUtil.executeOperation(getManagementClient().getControllerClient(), op, Outcome.SUCCESS);
    }

    @Test
    public void testStandardRbac() throws IOException {
        ModelControllerClient client = getManagementClient().getControllerClient();
        try {
            addMonitorUser(StandardRole.MONITOR, client);
            addMonitorUser(StandardRole.MAINTAINER, client);
            ModelNode op = Util.createEmptyOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION,
                    PathAddress.EMPTY_ADDRESS);
            RbacUtil.executeOperation(getClientForUser(RbacUtil.MONITOR_USER), op, Outcome.FAILED);
        } finally {
            try {
                removeMonitorUser(StandardRole.MAINTAINER, client);
            } finally {
                removeMonitorUser(StandardRole.MONITOR, client);
            }
        }
    }

    @Test
    public void testRunAsRoles() throws IOException {

        ModelNode op = Util.createEmptyOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION,
                PathAddress.EMPTY_ADDRESS);
        ModelNode roles = op.get(ModelDescriptionConstants.OPERATION_HEADERS).get(ModelDescriptionConstants.ROLES);
        roles.add(RbacUtil.MONITOR_USER);
        roles.add(RbacUtil.SUPERUSER_ROLE);

        RbacUtil.executeOperation(getManagementClient().getControllerClient(), op, Outcome.FAILED);
    }

    private void addMonitorUser(StandardRole role, ModelControllerClient client) throws IOException {
        RbacUtil.addRoleMapping(role.getFormalName(), client);
        RbacUtil.addRoleUser(role.getFormalName(), RbacUtil.MONITOR_USER, client);
    }

    private void removeMonitorUser(StandardRole role, ModelControllerClient client) throws IOException {
        try {
            RbacUtil.removeRoleUser(role.getFormalName(), RbacUtil.MONITOR_USER, client);
        } finally {
            RbacUtil.removeRoleMapping(role.getFormalName(), client);
        }
    }
}
