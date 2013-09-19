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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_READ;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_WRITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
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
public class VaultExpressionSensitivityTestCase extends AbstractRbacTestCase {
    private static final String VAULT_EXPRESSION_SENSITIVITY = "core-service=management/access=authorization/constraint=vault-expression";

    private static final String MASKED_DS = "subsystem=datasources/data-source=MaskedDS";
    private static final String NEW_CONNECTION_SQL = "new-connection-sql";
    private static final String VAULT_PASSWORD = "${VAULT::ds_ExampleDS::password::MWNjZWNkZjgtMWI2OC00MTMwLTlmNGItYWI0OTFiY2U4ZThiTElORV9CUkVBS3ZhdWx0}";

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addClass(VaultExpressionSensitivityTestCase.class);
    }

    @Test
    public void testReadSensitiveWriteSensitive() throws Exception {
        changeVaultExpressionSensitivity(true, true);
        try {
            test(RbacUtil.MONITOR_USER, false, false);
            test(RbacUtil.OPERATOR_USER, false, false);
            test(RbacUtil.MAINTAINER_USER, false, false);
            test(RbacUtil.DEPLOYER_USER, false, false);
            test(RbacUtil.ADMINISTRATOR_USER, true, true);
            test(RbacUtil.AUDITOR_USER, true, false);
            test(RbacUtil.SUPERUSER_USER, true, true);
        } finally {
            changeVaultExpressionSensitivity(null, null);
        }
    }

    @Test
    public void testReadNonSensitiveWriteSensitive() throws Exception {
        changeVaultExpressionSensitivity(false, true);
        try {
            test(RbacUtil.MONITOR_USER, true, false);
            test(RbacUtil.OPERATOR_USER, true, false);
            test(RbacUtil.MAINTAINER_USER, true, false);
            test(RbacUtil.DEPLOYER_USER, true, false);
            test(RbacUtil.ADMINISTRATOR_USER, true, true);
            test(RbacUtil.AUDITOR_USER, true, false);
            test(RbacUtil.SUPERUSER_USER, true, true);
        } finally {
            changeVaultExpressionSensitivity(null, null);
        }
    }

    @Test
    public void testReadSensitiveWriteNonSensitive() throws Exception {
        // read sensitive and write non-sensitive together makes very little, but still
        // note that the expectations are the same as in testReadSensitiveWriteSensitive

        changeVaultExpressionSensitivity(true, false);
        try {
            test(RbacUtil.MONITOR_USER, false, false);
            test(RbacUtil.OPERATOR_USER, false, false);
            test(RbacUtil.MAINTAINER_USER, false, false);
            test(RbacUtil.DEPLOYER_USER, false, false);
            test(RbacUtil.ADMINISTRATOR_USER, true, true);
            test(RbacUtil.AUDITOR_USER, true, false);
            test(RbacUtil.SUPERUSER_USER, true, true);
        } finally {
            changeVaultExpressionSensitivity(null, null);
        }
    }

    @Test
    public void testReadNonSensitiveWriteNonSensitive() throws Exception {
        changeVaultExpressionSensitivity(false, false);
        try {
            test(RbacUtil.MONITOR_USER, true, false);
            test(RbacUtil.OPERATOR_USER, true, false); // operator NO, it's a change in persistent configuration
            test(RbacUtil.MAINTAINER_USER, true, true);
            test(RbacUtil.DEPLOYER_USER, true, false);
            test(RbacUtil.ADMINISTRATOR_USER, true, true);
            test(RbacUtil.AUDITOR_USER, true, false);
            test(RbacUtil.SUPERUSER_USER, true, true);
        } finally {
            changeVaultExpressionSensitivity(null, null);
        }
    }

    private void test(String userName, boolean canRead, boolean canWrite) throws Exception {
        ModelControllerClient client = getClientForUser(userName);

        // read-resource
        ModelNode operation = createOpNode(MASKED_DS, READ_RESOURCE_OPERATION);
        ModelNode result = RbacUtil.executeOperation(client, operation, Outcome.SUCCESS);
        assertEquals(userName + " should " + (canRead ? "" : "NOT") + " be able to read", canRead, result.get(RESULT).hasDefined(NEW_CONNECTION_SQL));
        if (!canRead) {
            assertTrue(result.hasDefined(RESPONSE_HEADERS));
            List<ModelNode> filteredAttributes = result.get(RESPONSE_HEADERS, ACCESS_CONTROL).get(0).get("filtered-attributes").asList();
            assertTrue(filteredAttributes.contains(new ModelNode(NEW_CONNECTION_SQL)));
        }

        // read-attribute
        operation = createOpNode(MASKED_DS, READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(NEW_CONNECTION_SQL);
        RbacUtil.executeOperation(client, operation, canRead ? Outcome.SUCCESS : Outcome.UNAUTHORIZED);

        // write-attribute
        operation = createOpNode(MASKED_DS, WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(NEW_CONNECTION_SQL);
        operation.get(VALUE).set(VAULT_PASSWORD);
        RbacUtil.executeOperation(client, operation, canWrite ? Outcome.SUCCESS : Outcome.UNAUTHORIZED);
    }

    // test utils

    private void changeVaultExpressionSensitivity(Boolean readSensitive, Boolean writeSensitive) throws IOException {
        ModelControllerClient client = getManagementClient().getControllerClient();

        ModelNode operation;

        if (readSensitive != null) {
            operation = createOpNode(VAULT_EXPRESSION_SENSITIVITY, WRITE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set(CONFIGURED_REQUIRES_READ);
            operation.get(VALUE).set(readSensitive);
        } else {
            operation = createOpNode(VAULT_EXPRESSION_SENSITIVITY, UNDEFINE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set(CONFIGURED_REQUIRES_READ);
        }
        client.execute(operation);

        if (writeSensitive != null) {
            operation = createOpNode(VAULT_EXPRESSION_SENSITIVITY, WRITE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set(CONFIGURED_REQUIRES_WRITE);
            operation.get(VALUE).set(writeSensitive);
        } else {
            operation = createOpNode(VAULT_EXPRESSION_SENSITIVITY, UNDEFINE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set(CONFIGURED_REQUIRES_WRITE);
        }
        client.execute(operation);
    }
}
