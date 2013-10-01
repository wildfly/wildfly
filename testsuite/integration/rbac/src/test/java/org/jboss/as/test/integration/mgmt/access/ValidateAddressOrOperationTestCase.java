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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAP_GROUPS_TO_ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(UserRolesMappingServerSetupTask.StandardUsersSetup.class)
public class ValidateAddressOrOperationTestCase extends AbstractRbacTestCase {
    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addClass(ValidateAddressOrOperationTestCase.class);
    }

    @Test
    public void testMonitor() throws Exception {
        test(RbacUtil.MONITOR_USER, false, true, false, false, true, true);
    }

    @Test
    public void testOperator() throws Exception {
        test(RbacUtil.OPERATOR_USER, false, true, false, false, true, true);
    }

    @Test
    public void testMaintainer() throws Exception {
        test(RbacUtil.MAINTAINER_USER, false, true, false, false, true, true);
    }

    @Test
    public void testDeployer() throws Exception {
        test(RbacUtil.DEPLOYER_USER, false, true, false, false, true, true);
    }

    @Test
    public void testAdministrator() throws Exception {
        test(RbacUtil.ADMINISTRATOR_USER, true, true, true, true, true, true);
    }

    @Test
    public void testAuditor() throws Exception {
        test(RbacUtil.AUDITOR_USER, true, true, true, true, true, true);
    }

    @Test
    public void testSuperUser() throws Exception {
        test(RbacUtil.SUPERUSER_USER, true, true, true, true, true, true);
    }

    private void test(String userName,
                      boolean mgmtAuthorizationExpectation, boolean auditLogExpectation,
                      boolean securityRealmExpectation, boolean securityDomainExpectation,
                      boolean datasourceWithPlainPasswordExpectation, boolean datasourceWithMaskedPasswordExpectation) throws Exception {

        testValidateAddress(userName, mgmtAuthorizationExpectation, auditLogExpectation, securityRealmExpectation,
                securityDomainExpectation, datasourceWithPlainPasswordExpectation, datasourceWithMaskedPasswordExpectation);

        testValidateOperation(userName, mgmtAuthorizationExpectation, auditLogExpectation, securityRealmExpectation,
                securityDomainExpectation, datasourceWithPlainPasswordExpectation, datasourceWithMaskedPasswordExpectation);
    }

    private void testValidateAddress(String userName,
                                     boolean mgmtAuthorizationExpectation, boolean auditLogExpectation,
                                     boolean securityRealmExpectation, boolean securityDomainExpectation,
                                     boolean datasourceWithPlainPasswordExpectation, boolean datasourceWithMaskedPasswordExpectation) throws Exception {

        ModelControllerClient client = getClientForUser(userName);

        ModelNode address = new ModelNode();

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION);
        validateAddress(client, address, mgmtAuthorizationExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT);
        validateAddress(client, address, auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT).add(LOGGER, AUDIT_LOG);
        validateAddress(client, address, auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, "ManagementRealm");
        validateAddress(client, address, securityRealmExpectation);

        address.setEmptyList().add(SUBSYSTEM, "security").add("security-domain", "other");
        validateAddress(client, address, securityDomainExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "ExampleDS");
        validateAddress(client, address, datasourceWithPlainPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "MaskedDS");
        validateAddress(client, address, datasourceWithMaskedPasswordExpectation);
    }

    private void testValidateOperation(String userName,
                                       boolean mgmtAuthorizationExpectation, boolean auditLogExpectation,
                                       boolean securityRealmExpectation, boolean securityDomainExpectation,
                                       boolean datasourceWithPlainPasswordExpectation, boolean datasourceWithMaskedPasswordExpectation) throws Exception {

        ModelControllerClient client = getClientForUser(userName);

        ModelNode address = new ModelNode();

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION);
        validateOperation(client, readResource(address), mgmtAuthorizationExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION);
        validateOperation(client, readAttribute(address, PROVIDER), mgmtAuthorizationExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION);
        validateOperation(client, writeAttribute(address, PROVIDER, new ModelNode("simple")), mgmtAuthorizationExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT);
        validateOperation(client, readResource(address), auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT).add(LOGGER, AUDIT_LOG);
        validateOperation(client, readResource(address), auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT).add(LOGGER, AUDIT_LOG);
        validateOperation(client, readAttribute(address, ENABLED), auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUDIT).add(LOGGER, AUDIT_LOG);
        validateOperation(client, writeAttribute(address, ENABLED, new ModelNode(true)), auditLogExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, "ManagementRealm");
        validateOperation(client, readResource(address), securityRealmExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, "ManagementRealm");
        validateOperation(client, readAttribute(address, MAP_GROUPS_TO_ROLES), securityRealmExpectation);

        address.setEmptyList().add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, "ManagementRealm");
        validateOperation(client, writeAttribute(address, MAP_GROUPS_TO_ROLES, new ModelNode(true)), securityRealmExpectation);

        address.setEmptyList().add(SUBSYSTEM, "security").add("security-domain", "other");
        validateOperation(client, readResource(address), securityDomainExpectation);

        address.setEmptyList().add(SUBSYSTEM, "security").add("security-domain", "other");
        validateOperation(client, readAttribute(address, "cache-type"), securityDomainExpectation);

        address.setEmptyList().add(SUBSYSTEM, "security").add("security-domain", "other");
        validateOperation(client, writeAttribute(address, "cache-type", new ModelNode("infinispan")), securityDomainExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "ExampleDS");
        validateOperation(client, readResource(address), datasourceWithPlainPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "ExampleDS");
        validateOperation(client, readAttribute(address, "password"), datasourceWithPlainPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "ExampleDS");
        validateOperation(client, writeAttribute(address, "password", new ModelNode("new-password")), datasourceWithPlainPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "MaskedDS");
        validateOperation(client, readResource(address), datasourceWithMaskedPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "MaskedDS");
        validateOperation(client, readAttribute(address, "password"), datasourceWithMaskedPasswordExpectation);

        address.setEmptyList().add(SUBSYSTEM, "datasources").add("data-source", "MaskedDS");
        validateOperation(client, writeAttribute(address, "password", new ModelNode("new-password")), datasourceWithMaskedPasswordExpectation);
    }

    // test utils

    private static ModelNode readResource(ModelNode address) {
        ModelNode readResource = new ModelNode();
        readResource.get(OP).set(READ_RESOURCE_OPERATION);
        readResource.get(OP_ADDR).set(address);
        readResource.get(RECURSIVE).set(true);
        return readResource;
    }

    private static ModelNode readAttribute(ModelNode address, String attribute) {
        ModelNode readAttribute = new ModelNode();
        readAttribute.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readAttribute.get(OP_ADDR).set(address);
        readAttribute.get(NAME).set(attribute);
        return readAttribute;
    }

    private static ModelNode writeAttribute(ModelNode address, String attribute, ModelNode value) {
        ModelNode readAttribute = new ModelNode();
        readAttribute.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        readAttribute.get(OP_ADDR).set(address);
        readAttribute.get(NAME).set(attribute);
        readAttribute.get(VALUE).set(value);
        return readAttribute;
    }

    private static void validateAddress(ModelControllerClient client, ModelNode address, boolean expectedOutcome) throws IOException {
        ModelNode operation = Util.createOperation(ValidateAddressOperationHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        operation.get(VALUE).set(address);
        ModelNode result = client.execute(operation);

        assertModelNodeOnlyContainsKeys(result, OUTCOME, RESULT);
        assertModelNodeOnlyContainsKeys(result.get(RESULT), VALID, PROBLEM);
        assertEquals(expectedOutcome, result.get(RESULT, VALID).asBoolean());
        assertEquals(!expectedOutcome, result.get(RESULT).hasDefined(PROBLEM));
        if (!expectedOutcome) {
            assertTrue(result.get(RESULT, PROBLEM).asString().contains("not found"));
        }
    }

    private static void validateOperation(ModelControllerClient client, ModelNode validatedOperation, boolean expectedOutcome) throws IOException {
        ModelNode operation = Util.createOperation(VALIDATE_OPERATION, PathAddress.EMPTY_ADDRESS);
        operation.get(VALUE).set(validatedOperation);
        ModelNode result = client.execute(operation);

        assertModelNodeOnlyContainsKeys(result, OUTCOME, FAILURE_DESCRIPTION, ROLLED_BACK);
        if (expectedOutcome) {
            assertEquals(SUCCESS, result.get(OUTCOME).asString());
        } else {
            assertEquals(FAILED, result.get(OUTCOME).asString());
            assertTrue(result.get(FAILURE_DESCRIPTION).asString().contains("not found"));
        }
    }

    private static void assertModelNodeOnlyContainsKeys(ModelNode modelNode, String... keys) {
        Collection<String> expectedKeys = Arrays.asList(keys);
        Set<String> actualKeys = new HashSet<String>(modelNode.keys()); // need copy for modifications
        actualKeys.removeAll(expectedKeys);
        if (!actualKeys.isEmpty()) {
            fail("ModelNode contained additional keys: " + actualKeys);
        }
    }
}
