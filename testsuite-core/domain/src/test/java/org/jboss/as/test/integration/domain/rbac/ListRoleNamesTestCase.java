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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALL_ROLE_NAMES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STANDARD_ROLE_NAMES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
public class ListRoleNamesTestCase extends AbstractRbacTestCase {
    private static final String NEW_HOST_SCOPED_ROLE = "NewHostScopedTestRole";
    private static final String NEW_SERVER_GROUP_SCOPED_ROLE = "NewServerGroupScopedTestRole";

    private static final Set<String> STANDARD_ROLES_SET = new HashSet<String>();
    private static final Set<String> ALL_ROLES_SET_BASIC = new HashSet<String>();
    private static final Set<String> ALL_ROLES_SET_WITH_ADDITIONAL_ROLES = new HashSet<String>();

    static {
        STANDARD_ROLES_SET.addAll(Arrays.asList(RbacUtil.allStandardRoles()));

        ALL_ROLES_SET_BASIC.addAll(STANDARD_ROLES_SET);
        ALL_ROLES_SET_BASIC.addAll(Arrays.asList(AbstractHostScopedRolesTestCase.USERS));
        ALL_ROLES_SET_BASIC.addAll(Arrays.asList(AbstractServerGroupScopedRolesTestCase.USERS));

        ALL_ROLES_SET_WITH_ADDITIONAL_ROLES.addAll(ALL_ROLES_SET_BASIC);
        ALL_ROLES_SET_WITH_ADDITIONAL_ROLES.add(NEW_HOST_SCOPED_ROLE);
        ALL_ROLES_SET_WITH_ADDITIONAL_ROLES.add(NEW_SERVER_GROUP_SCOPED_ROLE);
    }

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
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(RbacUtil.ADMINISTRATOR_USER, false, masterClientConfig);
        test(client);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(RbacUtil.AUDITOR_USER, false, masterClientConfig);
        test(client);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(RbacUtil.SUPERUSER_USER, false, masterClientConfig);
        test(client);
    }

    private void test(ModelControllerClient client) throws Exception {
        assertUnorderedEquals(readStandardRoles(client), STANDARD_ROLES_SET);
        assertUnorderedEquals(readAllRoles(client), ALL_ROLES_SET_BASIC);

        try {
            addNewRoles();
            assertUnorderedEquals(readStandardRoles(client), STANDARD_ROLES_SET);
            assertUnorderedEquals(readAllRoles(client), ALL_ROLES_SET_WITH_ADDITIONAL_ROLES);
        } finally {
            removeNewRoles();
            assertUnorderedEquals(readStandardRoles(client), STANDARD_ROLES_SET);
            assertUnorderedEquals(readAllRoles(client), ALL_ROLES_SET_BASIC);
        }
    }

    // test utils

    private void addNewRoles() throws IOException {
        DomainClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        RbacUtil.addHostScopedRole(client, NEW_HOST_SCOPED_ROLE, RbacUtil.MONITOR_ROLE, AbstractHostScopedRolesTestCase.MASTER);
        RbacUtil.addServerGroupScopedRole(client, NEW_SERVER_GROUP_SCOPED_ROLE, RbacUtil.MONITOR_ROLE, AbstractServerGroupScopedRolesTestCase.SERVER_GROUP_A);
    }

    private void removeNewRoles() throws IOException {
        DomainClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        RbacUtil.removeHostScopedRole(client, NEW_HOST_SCOPED_ROLE);
        RbacUtil.removeServerGroupScopedRole(client, NEW_SERVER_GROUP_SCOPED_ROLE);
    }

    private static Set<String> readStandardRoles(ModelControllerClient client) throws IOException {
        return readRoles(client, STANDARD_ROLE_NAMES);
    }

    private static Set<String> readAllRoles(ModelControllerClient client) throws IOException {
        return readRoles(client, ALL_ROLE_NAMES);
    }

    private static Set<String> readRoles(ModelControllerClient client, String attribute) throws IOException {
        ModelNode operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION)
        ));
        operation.get(NAME).set(attribute);
        ModelNode result = RbacUtil.executeOperation(client, operation, Outcome.SUCCESS);

        Set<String> roles = new HashSet<String>();
        for (ModelNode roleNode : result.get(RESULT).asList()) {
            roles.add(roleNode.asString());
        }
        return roles;
    }

    private static void assertUnorderedEquals(Set<String> first, Set<String> second) {
        assertEquals("Two sets have different sizes: " + first + "; " + second, first.size(), second.size());

        Set<String> copyFirst = new HashSet<String>(first);
        Set<String> copySecond = new HashSet<String>(second);
        copyFirst.removeAll(second);
        copySecond.removeAll(first);
        if (!copyFirst.isEmpty() || !copySecond.isEmpty()) {
            fail("Two sets contain different elements: " + first + "; " + second);
        }
    }
}
