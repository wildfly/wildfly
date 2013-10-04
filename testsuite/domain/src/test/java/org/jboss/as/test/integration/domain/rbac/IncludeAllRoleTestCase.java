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

package org.jboss.as.test.integration.domain.rbac;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOSTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.MAPPED_ROLES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.VERBOSE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.WHOAMI;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleMapping;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.addRoleUser;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.executeOperation;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.removeRoleMapping;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.removeRoleUser;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.setRoleMappingIncludeAll;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.suites.FullRbacProviderTestSuite;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class IncludeAllRoleTestCase extends AbstractRbacTestCase {
    private static final String NEW_ROLE = "NewTestRole";
    private static final String NEW_USER = "NewTestUser";

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = FullRbacProviderTestSuite.createSupport(IncludeAllRoleTestCase.class.getSimpleName());
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.setup(domainClient);
        AbstractServerGroupScopedRolesTestCase.setupRoles(domainClient);
        RBACProviderServerGroupScopedRolesTestCase.ServerGroupRolesMappingSetup.INSTANCE.setup(domainClient);
        AbstractHostScopedRolesTestCase.setupRoles(domainClient);
        RBACProviderHostScopedRolesTestCase.HostRolesMappingSetup.INSTANCE.setup(domainClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        try {
            RBACProviderHostScopedRolesTestCase.HostRolesMappingSetup.INSTANCE.tearDown(domainClient);
        } finally {
            try {
                AbstractHostScopedRolesTestCase.tearDownRoles(domainClient);
            } finally {
                try {
                    RBACProviderServerGroupScopedRolesTestCase.ServerGroupRolesMappingSetup.INSTANCE.tearDown(domainClient);
                } finally {
                    try {
                        AbstractServerGroupScopedRolesTestCase.tearDownRoles(domainClient);
                    } finally {
                        try {
                            UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.tearDown(domainClient);
                        } finally {
                            FullRbacProviderTestSuite.stopSupport();
                            testSupport = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void configureRoles(ModelNode op, String[] roles) {
        // no-op. Role mapping is done based on the client's authenticated Subject
    }

    @Test
    public void testServerGroupScopedRoleShouldBeInStandardRole() throws Exception {
        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        addServerGroupScopedRole(client, NEW_ROLE, RbacUtil.MONITOR_ROLE, SERVER_GROUP_A);
        addRoleMapping(NEW_ROLE, client);
        addRoleUser(NEW_ROLE, NEW_USER, client);

        try {
            test(MAINTAINER_ROLE);
        } finally {
            removeRoleUser(NEW_ROLE, NEW_USER, client);
            removeRoleMapping(NEW_ROLE, client);
            removeServerGroupScopedRole(client, NEW_ROLE);
        }
    }

    @Test
    public void testServerGroupScopedRoleShouldBeInServerGroupScopedRole() throws Exception {
        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        addServerGroupScopedRole(client, NEW_ROLE, RbacUtil.MONITOR_ROLE, SERVER_GROUP_A);
        addRoleMapping(NEW_ROLE, client);
        addRoleUser(NEW_ROLE, NEW_USER, client);

        try {
            test(AbstractServerGroupScopedRolesTestCase.MAINTAINER_USER);
        } finally {
            removeRoleUser(NEW_ROLE, NEW_USER, client);
            removeRoleMapping(NEW_ROLE, client);
            removeServerGroupScopedRole(client, NEW_ROLE);
        }
    }

    @Test
    public void testHostScopedRoleShouldBeInStandardRole() throws Exception {
        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        addHostScopedRole(client, NEW_ROLE, RbacUtil.MONITOR_ROLE, MASTER);
        addRoleMapping(NEW_ROLE, client);
        addRoleUser(NEW_ROLE, NEW_USER, client);

        try {
            test(MAINTAINER_ROLE);
        } finally {
            removeRoleUser(NEW_ROLE, NEW_USER, client);
            removeRoleMapping(NEW_ROLE, client);
            removeHostScopedRole(client, NEW_ROLE);
        }
    }

    @Test
    public void testHostScopedRoleShouldBeInHostScopedRole() throws Exception {
        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        addHostScopedRole(client, NEW_ROLE, RbacUtil.MONITOR_ROLE, MASTER);
        addRoleMapping(NEW_ROLE, client);
        addRoleUser(NEW_ROLE, NEW_USER, client);

        try {
            test(AbstractHostScopedRolesTestCase.MAINTAINER_USER);
        } finally {
            removeRoleUser(NEW_ROLE, NEW_USER, client);
            removeRoleMapping(NEW_ROLE, client);
            removeHostScopedRole(client, NEW_ROLE);
        }
    }

    private void test(String includeAllRole) throws Exception {
        ModelControllerClient mgmtClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        ModelControllerClient newUserClient = getClientForUser(NEW_USER, false, masterClientConfig);
        assertIsCallerInRole(newUserClient, NEW_ROLE, true);
        assertIsCallerInRole(newUserClient, includeAllRole, false);
        removeClientForUser(NEW_USER, false);

        setRoleMappingIncludeAll(mgmtClient, includeAllRole, true);

        newUserClient = getClientForUser(NEW_USER, false, masterClientConfig);
        assertIsCallerInRole(newUserClient, NEW_ROLE, true);
        assertIsCallerInRole(newUserClient, includeAllRole, true);
        removeClientForUser(NEW_USER, false);

        setRoleMappingIncludeAll(mgmtClient, includeAllRole, false);

        newUserClient = getClientForUser(NEW_USER, false, masterClientConfig);
        assertIsCallerInRole(newUserClient, NEW_ROLE, true);
        assertIsCallerInRole(newUserClient, includeAllRole, false);
        removeClientForUser(NEW_USER, false);
    }

    // test utils

    // must reimplement here as scoped roles can't call .../role-mappping=*:is-caller-in-role
    private static void assertIsCallerInRole(ModelControllerClient client, String role, boolean expectation) throws IOException {
        boolean result = isCallerInRole(client, role);
        assertEquals("expected caller to be in role " + role, expectation, result);
    }

    private static boolean isCallerInRole(ModelControllerClient client, String role) throws IOException {
        ModelNode operation = Util.createOperation(WHOAMI, EMPTY_ADDRESS);
        operation.get(VERBOSE).set(true);
        ModelNode result = RbacUtil.executeOperation(client, operation, Outcome.SUCCESS);

        if (!result.get(RESULT).hasDefined(MAPPED_ROLES)) {
            return false;
        }

        List<ModelNode> actualRoles = result.get(RESULT, MAPPED_ROLES).asList();
        for (ModelNode actualRole : actualRoles) {
            if (role.equalsIgnoreCase(actualRole.asString())) {
                return true;
            }
        }

        return false;
    }

    private static void addServerGroupScopedRole(ModelControllerClient client, String roleName, String baseRole,
                                                 String... serverGroups) throws IOException {

        ModelNode operation = Util.createOperation(ADD, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(SERVER_GROUP_SCOPED_ROLE, roleName)
        ));
        operation.get(BASE_ROLE).set(baseRole);
        ModelNode serverGroupsModelNode = operation.get(SERVER_GROUPS);
        for (String serverGroup : serverGroups) {
            serverGroupsModelNode.add(serverGroup);
        }
        executeOperation(client, operation, Outcome.SUCCESS);
    }

    private static void removeServerGroupScopedRole(ModelControllerClient client, String roleName) throws IOException {
        ModelNode operation = Util.createOperation(REMOVE, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(SERVER_GROUP_SCOPED_ROLE, roleName)
        ));
        executeOperation(client, operation, Outcome.SUCCESS);
    }

    private static void addHostScopedRole(ModelControllerClient client, String roleName, String baseRole,
                                          String... hosts) throws IOException {

        ModelNode operation = Util.createOperation(ADD, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(HOST_SCOPED_ROLE, roleName)
        ));
        operation.get(BASE_ROLE).set(baseRole);
        ModelNode hostsModelNode = operation.get(HOSTS);
        for (String host : hosts) {
            hostsModelNode.add(host);
        }
        executeOperation(client, operation, Outcome.SUCCESS);
    }

    private static void removeHostScopedRole(ModelControllerClient client, String roleName) throws IOException {
        ModelNode operation = Util.createOperation(REMOVE, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(HOST_SCOPED_ROLE, roleName)
        ));
        executeOperation(client, operation, Outcome.SUCCESS);
    }
}
