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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_ROLE;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.interfaces.CliManagementInterface;
import org.jboss.as.test.integration.management.interfaces.HttpManagementInterface;
import org.jboss.as.test.integration.management.interfaces.ManagementInterface;
import org.jboss.as.test.integration.management.interfaces.NativeManagementInterface;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author jcechace
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(RoleMappingRuntimeReconfigurationTestCase.BasicUsersSetup.class)
public class RoleMappingRuntimeReconfigurationTestCase {
    private static final String ROLE_MAPPING_ADDRESS_BASE = "core-service=management/access=authorization/role-mapping=";
    private static final String OPERATOR_ROLE_MAPPING = ROLE_MAPPING_ADDRESS_BASE + "Operator";
    private static final String MAINTAINER_ROLE_MAPPING = ROLE_MAPPING_ADDRESS_BASE + "Maintainer";
    private static final String ROLE_INCLUSION_USER = MAINTAINER_ROLE_MAPPING + "/include=user-";
    private static final String TEST_USER = "testUser";
    private static final String TEST_USER_2 = "testUser2";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClass(RoleMappingRuntimeReconfigurationTestCase.class);
        return jar;
    }

    @Before
    public void setUp() throws IOException {
        addIfNotExists(MAINTAINER_ROLE_MAPPING, managementClient.getControllerClient());
        addIfNotExists(ROLE_INCLUSION_USER + TEST_USER_2, managementClient.getControllerClient(),
                "name=" + TEST_USER_2, "type=user");
    }

    @After
    public void tearDown() throws IOException {
        removeIfExists(OPERATOR_ROLE_MAPPING, managementClient.getControllerClient());
        removeIfExists(ROLE_INCLUSION_USER + TEST_USER, managementClient.getControllerClient());
    }

    @Test
    public void testNativeInterface() throws IOException {
        ManagementInterface client = NativeManagementInterface.create(
                managementClient.getMgmtAddress(), managementClient.getMgmtPort(),
                RbacUtil.SUPERUSER_USER, RbacAdminCallbackHandler.STD_PASSWORD
        );
        test(client);
    }

    @Test
    public void testHttpInterface() throws IOException {
        ManagementInterface client = HttpManagementInterface.create(
                managementClient.getMgmtAddress(), managementClient.getMgmtPort(),
                RbacUtil.SUPERUSER_USER, RbacAdminCallbackHandler.STD_PASSWORD
        );
        test(client);
    }

    @Test
    public void testCliInterface() throws IOException {
        ManagementInterface client = CliManagementInterface.create(
                managementClient.getMgmtAddress(), managementClient.getMgmtPort(),
                RbacUtil.SUPERUSER_USER, RbacAdminCallbackHandler.STD_PASSWORD
        );
        test(client);
    }

    private void test(ManagementInterface client) throws IOException {
        addRoleMapping(client);
        removeUserInclusion(client);
        addUserInclusion(client);
        removeRoleMapping(client);
    }

    // test utils

    private void addRoleMapping(ManagementInterface client) throws IOException {
        ModelNode op = createOpNode(OPERATOR_ROLE_MAPPING, ADD);
        RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        checkIfExists(OPERATOR_ROLE_MAPPING, true, managementClient.getControllerClient());
    }

    private void removeRoleMapping(ManagementInterface client) throws IOException {
        ModelNode op = createOpNode(MAINTAINER_ROLE_MAPPING, REMOVE);
        RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        checkIfExists(MAINTAINER_ROLE_MAPPING, false, managementClient.getControllerClient());
    }

    private void addUserInclusion(ManagementInterface client) throws IOException {
        String address = ROLE_INCLUSION_USER + TEST_USER;
        ModelNode op = createOpNode(address, ADD);
        op.get(NAME).set(TEST_USER);
        op.get(TYPE).set(USER);
        RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        checkIfExists(address, true, managementClient.getControllerClient());
    }

    private void removeUserInclusion(ManagementInterface client) throws IOException {
        String address = ROLE_INCLUSION_USER + TEST_USER_2;
        ModelNode op = createOpNode(address, REMOVE);
        RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        checkIfExists(address, false, managementClient.getControllerClient());
    }

    private static void addIfNotExists(String address, ModelControllerClient client, String... attributePairs) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        if (FAILED.equals(client.execute(readOp).get(OUTCOME).asString())) {
            ModelNode addOp = createOpNode(address, ADD);
            for (String attr : attributePairs) {
                String[] parts = attr.split("=");
                addOp.get(parts[0]).set(parts[1]);
            }
            RbacUtil.executeOperation(client, addOp, Outcome.SUCCESS);
        }
        checkIfExists(address, true, client);
    }

    private static void removeIfExists(String address, ModelControllerClient client) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        ModelNode result = client.execute(readOp);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            ModelNode removeOp = createOpNode(address, REMOVE);
            RbacUtil.executeOperation(client, removeOp, Outcome.SUCCESS);
        }
        checkIfExists(address, false, client);
    }

    private static void checkIfExists(String address, boolean shouldExist, ModelControllerClient client) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        ModelNode result = client.execute(readOp);
        String expected = shouldExist ? SUCCESS : FAILED;
        assertEquals(expected, result.get(OUTCOME).asString());
    }

    /**
     * {@link UserRolesMappingServerSetupTask} that adds a single user mapping for each standard
     * role, with the username the same as the role name.
     */
    public static class BasicUsersSetup extends UserRolesMappingServerSetupTask implements ServerSetupTask{

        static {
            Map<String, Set<String>> rolesToUsers = new HashMap<String, Set<String>>();
            rolesToUsers.put(SUPERUSER_ROLE, Collections.singleton(SUPERUSER_USER));
            STANDARD_USERS = rolesToUsers;
        }

        private static final Map<String, Set<String>> STANDARD_USERS;

        public static final StandardUsersSetup INSTANCE = new StandardUsersSetup();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            setup(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            tearDown(managementClient.getControllerClient());
        }

        public BasicUsersSetup() {
            super(STANDARD_USERS);
        }
    }
}
