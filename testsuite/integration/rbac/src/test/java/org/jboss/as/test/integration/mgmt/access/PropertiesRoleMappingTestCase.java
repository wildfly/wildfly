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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.domain.management.ModelDescriptionConstants.IS_CALLER_IN_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleMapping;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleUser;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.executeOperation;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.removeRoleUser;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class is also used in the LDAP test cases.
 *
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Arquillian.class)
public class PropertiesRoleMappingTestCase extends AbstractRbacTestCase {
    @Test
    public void testMonitor() throws Exception {
        test("UserMappedToGroupMonitor", MONITOR_ROLE);
    }

    @Test
    public void testOperator() throws Exception {
        test("UserMappedToGroupOperator", OPERATOR_ROLE);
    }

    @Test
    public void testMaintainer() throws Exception {
        test("UserMappedToGroupMaintainer", MAINTAINER_ROLE);
    }

    @Test
    public void testDeployer() throws Exception {
        test("UserMappedToGroupDeployer", DEPLOYER_ROLE);
    }

    @Test
    public void testAdministrator() throws Exception {
        test("UserMappedToGroupAdministrator", ADMINISTRATOR_ROLE);
    }

    @Test
    public void testAuditor() throws Exception {
        test("UserMappedToGroupAuditor", AUDITOR_ROLE);
    }

    @Test
    public void testSuperUser() throws Exception {
        test("UserMappedToGroupSuperUser", SUPERUSER_ROLE);
    }

    @Test
    public void testOperatorAndMonitor() throws Exception {
        test("UserMappedToGroupOperatorAndMonitor", OPERATOR_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testMaintainerAndMonitor() throws Exception {
        test("UserMappedToGroupMaintainerAndMonitor", MAINTAINER_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testDeployerAndMonitor() throws Exception {
        test("UserMappedToGroupDeployerAndMonitor", DEPLOYER_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testAdministratorAndMonitor() throws Exception {
        test("UserMappedToGroupAdministratorAndMonitor", ADMINISTRATOR_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testAuditorAndMonitor() throws Exception {
        test("UserMappedToGroupAuditorAndMonitor", AUDITOR_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testSuperUserAndMonitor() throws Exception {
        test("UserMappedToGroupSuperUserAndMonitor", SUPERUSER_ROLE, MONITOR_ROLE);
    }

    @Test
    public void testOperatorAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupOperatorAndMonitorAndExcludedFromGroupMonitor", OPERATOR_ROLE);
    }

    @Test
    public void testMaintainerAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupMaintainerAndMonitorAndExcludedFromGroupMonitor", MAINTAINER_ROLE);
    }

    @Test
    public void testDeployerAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupDeployerAndMonitorAndExcludedFromGroupMonitor", DEPLOYER_ROLE);
    }

    @Test
    public void testAdministratorAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupAdministratorAndMonitorAndExcludedFromGroupMonitor", ADMINISTRATOR_ROLE);
    }

    @Test
    public void testAuditorAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupAuditorAndMonitorAndExcludedFromGroupMonitor", AUDITOR_ROLE);
    }

    @Test
    public void testSuperUserAndMonitorAndExcludedFromMonitor() throws Exception {
        test("UserMappedToGroupSuperUserAndMonitorAndExcludedFromGroupMonitor", SUPERUSER_ROLE);
    }

    @Test
    public void testOperatorAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupOperatorAndMonitorAndExcludingGroup", OPERATOR_ROLE);
    }

    @Test
    public void testMaintainerAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupMaintainerAndMonitorAndExcludingGroup", MAINTAINER_ROLE);
    }

    @Test
    public void testDeployerAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupDeployerAndMonitorAndExcludingGroup", DEPLOYER_ROLE);
    }

    @Test
    public void testAdministratorAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupAdministratorAndMonitorAndExcludingGroup", ADMINISTRATOR_ROLE);
    }

    @Test
    public void testAuditorAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupAuditorAndMonitorAndExcludingGroup", AUDITOR_ROLE);
    }

    @Test
    public void testSuperUserAndMonitorAndExcludingGroup() throws Exception {
        test("UserMappedToGroupSuperUserAndMonitorAndExcludingGroup", SUPERUSER_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationMonitor() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupMonitor", MONITOR_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationOperator() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupOperator", OPERATOR_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationMaintainer() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupMaintainer", MAINTAINER_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationDeployer() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupDeployer", DEPLOYER_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationAdministrator() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupAdministrator", ADMINISTRATOR_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationAuditor() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupAuditor", AUDITOR_ROLE);
    }

    @Test
    public void testRuntimeReconfigurationSuperUser() throws Exception {
        testRuntimeReconfiguration("UserMappedToGroupSuperUser", SUPERUSER_ROLE);
    }

    private static final String[] ALL_ROLES = {
            MONITOR_ROLE,
            OPERATOR_ROLE,
            MAINTAINER_ROLE,
            DEPLOYER_ROLE,
            ADMINISTRATOR_ROLE,
            AUDITOR_ROLE,
            SUPERUSER_ROLE
    };

    private void test(String user, String... expectedRoles) throws IOException {
        Set<String> expectedRolesSet = new HashSet<String>(Arrays.asList(expectedRoles));

        ModelControllerClient client = getClientForUser(user);
        for (String role : ALL_ROLES) {
            assertIsCallerInRole(client, role, expectedRolesSet.contains(role));
        }
    }

    private void testRuntimeReconfiguration(String user, String originalRole) throws Exception {
        Set<String> allRolesWithoutTheOriginal = new HashSet<String>(Arrays.asList(ALL_ROLES));
        allRolesWithoutTheOriginal.remove(originalRole);

        for (String newRole : allRolesWithoutTheOriginal) {
            test(user, originalRole);
            addUserToRole(user, newRole);
            try {
                test(user, originalRole, newRole);
            } finally {
                removeUserFromRole(user, newRole);
            }
            test(user, originalRole);
        }
    }

    // test utils

    private static void assertIsCallerInRole(ModelControllerClient client, String role, boolean expectedOutcome) throws IOException {
        ModelNode operation = Util.createOperation(IS_CALLER_IN_ROLE, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(ROLE_MAPPING, role)
        ));

        ModelNode result = executeOperation(client, operation, Outcome.SUCCESS);
        assertEquals("role " + role, expectedOutcome, result.get(RESULT).asBoolean());
    }

    private void addUserToRole(String user, String role) throws IOException {
        ModelControllerClient client = getManagementClient().getControllerClient();
        addRoleMapping(role, client); // make sure the role mapping exists
        addRoleUser(role, user, client);
    }

    private void removeUserFromRole(String user, String role) throws IOException {
        ModelControllerClient client = getManagementClient().getControllerClient();
        addRoleMapping(role, client); // make sure the role mapping exists
        removeRoleUser(role, user, client);
    }
}
