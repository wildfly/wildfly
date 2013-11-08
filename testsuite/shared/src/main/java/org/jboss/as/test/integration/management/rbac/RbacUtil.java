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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOSTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.domain.management.ModelDescriptionConstants.IS_CALLER_IN_ROLE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.interfaces.ManagementInterface;
import org.jboss.dmr.ModelNode;

/**
 * Utilities related to RBAC testing.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class RbacUtil {

    public static final String MONITOR_USER = "Monitor";
    public static final String OPERATOR_USER = "Operator";
    public static final String MAINTAINER_USER = "Maintainer";
    public static final String DEPLOYER_USER = "Deployer";
    public static final String ADMINISTRATOR_USER = "Administrator";
    public static final String AUDITOR_USER = "Auditor";
    public static final String SUPERUSER_USER = "SuperUser";

    public static final String MONITOR_ROLE = "Monitor";
    public static final String OPERATOR_ROLE = "Operator";
    public static final String MAINTAINER_ROLE = "Maintainer";
    public static final String DEPLOYER_ROLE = "Deployer";
    public static final String ADMINISTRATOR_ROLE = "Administrator";
    public static final String AUDITOR_ROLE = "Auditor";
    public static final String SUPERUSER_ROLE = "SuperUser";

    public static final String ROLE_MAPPING_ADDRESS_BASE = "core-service=management/access=authorization/role-mapping=";
    private static final String ROLE_MAPPING_USER_INCLUDE_ADDRESS_BASE = "/include=user-";
    private static final String ROLE_MAPPING_GROUP_INCLUDE_ADDRESS_BASE = "/include=group-";

    private RbacUtil() {
        // prevent instantiation
    }

    public static ModelNode executeOperation(ModelControllerClient client, ModelNode operation, Outcome expectedOutcome)
        throws IOException {
        ModelNode result = client.execute(operation);
        return checkOperationResult(operation, result, expectedOutcome);
    }

    public static ModelNode executeOperation(ManagementInterface client, ModelNode operation, Outcome expectedOutcome) throws IOException {
        ModelNode result = client.execute(operation);
        return checkOperationResult(operation, result, expectedOutcome);
    }

    public static ModelNode checkOperationResult(ModelNode operation, ModelNode result, Outcome expectedOutcome) {
        String outcome = result.get(OUTCOME).asString();
        switch (expectedOutcome) {
            case SUCCESS:
                if (!SUCCESS.equals(outcome)) {
                    System.out.println("Failed: " + operation);
                    System.out.print("Result: " + result);
                    fail(result.get(FAILURE_DESCRIPTION).asString());
                }
                break;
            case UNAUTHORIZED: {
                if (!FAILED.equals(outcome)) {
                    fail("Didn't fail: " + result.asString());
                }
                if (!result.get(FAILURE_DESCRIPTION).asString().contains("13456") && !result.asString().contains("11360")
                        && !result.asString().contains("11361")  && !result.asString().contains("11362")  && !result.asString().contains("11363")) {
                    fail("Incorrect failure type: " + result.asString());
                }
                break;
            }
            case HIDDEN: {
                if (!FAILED.equals(outcome)) {
                    fail("Didn't fail: " + result.asString());
                }
                String failureDesc = result.get(FAILURE_DESCRIPTION).asString();
                if (!failureDesc.contains("14807") && !failureDesc.contains("14883") && !failureDesc.contains("11340")) {
                    fail("Incorrect failure type: " + result.asString());
                }
                break;
            }
            case FAILED: {
                if (!FAILED.equals(outcome)) {
                    fail("Didn't fail: " + result.asString());
                }
                String failureDesc = result.get(FAILURE_DESCRIPTION).asString();
                if (failureDesc.contains("14807") || failureDesc.contains("14883")
                        || failureDesc.contains("13456") || failureDesc.contains("11340")) {
                    fail("Incorrect failure type: " + result.asString());
                }
                break;
            }
            default:
                throw new IllegalStateException();
        }
        return result;
    }

    public static void addRoleMapping(String role, ModelControllerClient client) throws IOException {
        String address = ROLE_MAPPING_ADDRESS_BASE + role;
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        if (FAILED.equals(client.execute(readOp).get(OUTCOME).asString())) {
            ModelNode addOp = createOpNode(address, ADD);
            executeOperation(client, addOp, Outcome.SUCCESS);
        }
    }

    public static void addRoleUser(String role, String user, ModelControllerClient client) throws IOException {
        ModelNode op = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role + ROLE_MAPPING_USER_INCLUDE_ADDRESS_BASE + user, ADD);
        op.get(TYPE).set(USER);
        op.get(NAME).set(user);
        executeOperation(client, op, Outcome.SUCCESS);
    }

    public static void removeRoleUser(String role, String user, ModelControllerClient client) throws IOException {
        ModelNode op = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role + ROLE_MAPPING_USER_INCLUDE_ADDRESS_BASE + user, REMOVE);
        executeOperation(client, op, Outcome.SUCCESS);
    }

    public static void addRoleGroup(String role, String group, ModelControllerClient client) throws IOException {
        ModelNode op = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role + ROLE_MAPPING_GROUP_INCLUDE_ADDRESS_BASE + group, ADD);
        op.get(TYPE).set(GROUP);
        op.get(NAME).set(group);
        executeOperation(client, op, Outcome.SUCCESS);
    }

    public static void removeRoleGroup(String role, String group, ModelControllerClient client) throws IOException {
        ModelNode op = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role + ROLE_MAPPING_GROUP_INCLUDE_ADDRESS_BASE + group, REMOVE);
        executeOperation(client, op, Outcome.SUCCESS);
    }

    public static void removeRoleMapping(String role, ModelControllerClient client) throws IOException {
        ModelNode op = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role, REMOVE);
        executeOperation(client, op, Outcome.SUCCESS);
    }

    public static void addRoleHeader(ModelNode operation, String... roles) {
        ModelNode header = operation.get(OPERATION_HEADERS, ROLES);
        for (String role : roles) {
            header.add(role);
        }
    }

    public static String[] allStandardRoles() {
        return new String[] {MONITOR_ROLE, OPERATOR_ROLE, MAINTAINER_ROLE, DEPLOYER_ROLE, ADMINISTRATOR_ROLE, AUDITOR_ROLE, SUPERUSER_ROLE};
    }

    public static void assertIsCallerInRole(ModelControllerClient client, String role, boolean expectedOutcome) throws IOException {
        ModelNode operation = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role, IS_CALLER_IN_ROLE);
        ModelNode result = executeOperation(client, operation, Outcome.SUCCESS);
        assertEquals("expected caller to be in role " + role, expectedOutcome, result.get(RESULT).asBoolean());
    }

    public static void setRoleMappingIncludeAll(ModelControllerClient client, String role, boolean includeAll) throws IOException {
        ModelNode operation = createOpNode(ROLE_MAPPING_ADDRESS_BASE + role, WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(INCLUDE_ALL);
        operation.get(VALUE).set(includeAll);
        executeOperation(client, operation, Outcome.SUCCESS);
    }

    public static void addServerGroupScopedRole(ModelControllerClient client, String roleName, String baseRole,
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

    public static void removeServerGroupScopedRole(ModelControllerClient client, String roleName) throws IOException {
        ModelNode operation = Util.createOperation(REMOVE, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(SERVER_GROUP_SCOPED_ROLE, roleName)
        ));
        executeOperation(client, operation, Outcome.SUCCESS);
    }

    public static void addHostScopedRole(ModelControllerClient client, String roleName, String baseRole,
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

    public static void removeHostScopedRole(ModelControllerClient client, String roleName) throws IOException {
        ModelNode operation = Util.createOperation(REMOVE, pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, ModelDescriptionConstants.AUTHORIZATION),
                pathElement(HOST_SCOPED_ROLE, roleName)
        ));
        executeOperation(client, operation, Outcome.SUCCESS);
    }
}
