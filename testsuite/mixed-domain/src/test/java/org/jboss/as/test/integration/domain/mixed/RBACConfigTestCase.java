/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_APPLICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_WRITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOSTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that RBAC configuration is properly handled in a mixed-domain.
 *
 * @author Brian Stansberry
 */
public class RBACConfigTestCase {

    private static final PathAddress RBAC_BASE = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
            PathElement.pathElement(ACCESS, AUTHORIZATION));
    private static final PathAddress SERVER_ONE = PathAddress.pathAddress(PathElement.pathElement(HOST, "slave"),
            PathElement.pathElement(RUNNING_SERVER, "server-one"));


    private static ModelControllerClient masterClient;
    private static ModelControllerClient slaveClient;

    @Before
    public void init() throws Exception {
        DomainTestSupport support = KernelBehaviorTestSuite.getSupport(this.getClass());
        masterClient = support.getDomainMasterLifecycleUtil().getDomainClient();
        slaveClient = support.getDomainSlaveLifecycleUtil().getDomainClient();
    }

    @AfterClass
    public static synchronized void afterClass() {
        KernelBehaviorTestSuite.afterClass();
        masterClient = slaveClient = null;
    }

    @Test
    public void testWriteRBACProvider() throws IOException {
        modifyTest(RBAC_BASE, PROVIDER);
    }

    @Test
    public void testAddRoleMapping() throws IOException {
        PathAddress address = RBAC_BASE.append(ROLE_MAPPING, "Operator");
        String attribute = INCLUDE_ALL;
        ModelNode value = new ModelNode(true);
        ModelNode addOp = Util.createAddOperation(address);
        addOp.get(attribute).set(value);

        addTest(address, attribute, value, addOp);
    }

    @Test
    public void testAddHostScopedRole() throws IOException {
        PathAddress address = RBAC_BASE.append(HOST_SCOPED_ROLE, "WFCORE-1622_H");
        String attribute = BASE_ROLE;
        ModelNode value = new ModelNode("Operator");
        ModelNode addOp = Util.createAddOperation(address);
        addOp.get(attribute).set(value);
        addOp.get(HOSTS).add("slave");

        addTest(address, attribute, value, addOp);
    }

    @Test
    public void testAddServerGroupScopedRole() throws IOException {
        PathAddress address = RBAC_BASE.append(SERVER_GROUP_SCOPED_ROLE, "WFCORE-1622_S");
        String attribute = BASE_ROLE;
        ModelNode value = new ModelNode("Operator");
        ModelNode addOp = Util.createAddOperation(address);
        addOp.get(attribute).set(value);
        addOp.get(SERVER_GROUPS).add("main-server-group");

        addTest(address, attribute, value, addOp);
    }

    @Test
    public void testModifySensitivityConstraint() throws IOException {

        PathAddress mapping = RBAC_BASE.append(CONSTRAINT, SENSITIVITY_CLASSIFICATION)
                .append(TYPE, CORE)
                .append(CLASSIFICATION, "socket-config");
        modifyTest(mapping, CONFIGURED_REQUIRES_WRITE, new ModelNode(true), true);
    }

    @Test
    public void testModifyServerSSLSensitivityConstraint() throws IOException {

        PathAddress mapping = RBAC_BASE.append(CONSTRAINT, SENSITIVITY_CLASSIFICATION)
                .append(TYPE, CORE)
                .append(CLASSIFICATION, "server-ssl");
        modifyTest(mapping, CONFIGURED_REQUIRES_WRITE, new ModelNode(true), getSupportsServerSSL());
    }

    @Test
    public void testModifyApplicationConstraint() throws IOException {

        PathAddress mapping = RBAC_BASE.append(CONSTRAINT, APPLICATION_CLASSIFICATION)
                .append(TYPE, CORE)
                .append(CLASSIFICATION, "deployment");
        modifyTest(mapping, CONFIGURED_APPLICATION, new ModelNode(true), true);
    }

    @Test
    public void testModifySensitiveExpressionsConstraint() throws IOException {

        PathAddress mapping = RBAC_BASE.append(CONSTRAINT, VAULT_EXPRESSION);
        modifyTest(mapping, CONFIGURED_REQUIRES_WRITE, new ModelNode(true), true);
    }

    /** Override this to return false in subclasses that test EAP < 6.4.7 */
    protected boolean getSupportsServerSSL() {
        return true;
    }

    private void modifyTest(PathAddress base, String attribute) throws IOException {
        ModelNode masterValue = executeForResult(Util.getReadAttributeOperation(base, attribute), masterClient);
        ModelNode slaveValue = executeForResult(Util.getReadAttributeOperation(base, attribute), slaveClient);
        assertEquals(masterValue, slaveValue);
        ModelNode serverValue = executeForResult(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
        assertEquals(masterValue, serverValue);

        // Write the same value, so we don't need to clean up.
        executeForResult(Util.getWriteAttributeOperation(base, attribute, masterValue), masterClient);

        ModelNode newMasterValue = executeForResult(Util.getReadAttributeOperation(base, attribute), masterClient);
        assertEquals(masterValue, newMasterValue);
        slaveValue = executeForResult(Util.getReadAttributeOperation(base, attribute), slaveClient);
        assertEquals(masterValue, slaveValue);
        serverValue = executeForResult(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
        assertEquals(masterValue, serverValue);
    }

    private void modifyTest(PathAddress base, String attribute, ModelNode newValue, boolean expectSlaveEffect) throws IOException {
        ModelNode masterValue = executeForResult(Util.getReadAttributeOperation(base, attribute), masterClient);
        if (expectSlaveEffect) {
            ModelNode slaveValue = executeForResult(Util.getReadAttributeOperation(base, attribute), slaveClient);
            assertEquals(masterValue, slaveValue);
            ModelNode serverValue = executeForResult(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
            assertEquals(masterValue, serverValue);
        } else {
            executeForFailure(Util.getReadAttributeOperation(base, attribute), slaveClient);
            executeForFailure(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
        }

        // Write the same value, so we don't need to clean up.
        Throwable caught = null;
        executeForResult(Util.getWriteAttributeOperation(base, attribute, newValue), masterClient);
        try {
            ModelNode newMasterValue = executeForResult(Util.getReadAttributeOperation(base, attribute), masterClient);
            assertEquals(newValue, newMasterValue);
            if (expectSlaveEffect) {
                ModelNode slaveValue = executeForResult(Util.getReadAttributeOperation(base, attribute), slaveClient);
                assertEquals(newValue, slaveValue);
                ModelNode serverValue = executeForResult(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
                assertEquals(newValue, serverValue);
            } else {
                executeForFailure(Util.getReadAttributeOperation(base, attribute), slaveClient);
                executeForFailure(Util.getReadAttributeOperation(SERVER_ONE.append(base), attribute), masterClient);
            }
        } catch (Exception | Error e) {
            caught = e;
            throw e;
        } finally {
            if (!newValue.equals(masterValue)) {
                try {
                    executeForResult(Util.getWriteAttributeOperation(base, attribute, masterValue), masterClient);
                } catch (RuntimeException e) {
                    if (caught == null) {
                        throw e;
                    } else {
                        e.printStackTrace(System.out);
                    }
                }
            }
        }

    }

    private void addTest(PathAddress address, String attribute, ModelNode value, ModelNode addOp) {
        Throwable caught = null;
        executeForResult(addOp, masterClient);
        try {
            ModelNode masterValue = executeForResult(Util.getReadAttributeOperation(address, attribute), masterClient);
            assertEquals(value, masterValue);
            ModelNode slaveValue = executeForResult(Util.getReadAttributeOperation(address, attribute), slaveClient);
            assertEquals(value, slaveValue);
            ModelNode serverValue = executeForResult(Util.getReadAttributeOperation(SERVER_ONE.append(address), attribute), masterClient);
            assertEquals(value, serverValue);
        } catch (Exception | Error e) {
            caught = e;
            throw e;
        } finally {
            try {
                executeForResult(Util.createRemoveOperation(address), masterClient);
            } catch (RuntimeException e) {
                if (caught == null) {
                    throw e;
                } else {
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    private ModelNode executeForResult(ModelNode op, ModelControllerClient client) {
        try {
            ModelNode response = client.execute(op);
            assertEquals(op.toString() + '\n' + response.toString(), SUCCESS, response.get(OUTCOME).asString());
            return response.get(RESULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode executeForFailure(ModelNode op, ModelControllerClient client) {
        try {
            ModelNode response = client.execute(op);
            assertEquals(op.toString() + '\n' + response.toString(), FAILED, response.get(OUTCOME).asString());
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
