/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that the validate-operation operation works as it should
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ValidateOperationOperationTestCase extends AbstractMgmtTestBase {


    private static final String MASTER = "master";
    private static final String SLAVE = "slave";
    private static final String MASTER_SERVER = "main-one";
    private static final String SLAVE_SERVER = "main-three";

    private static ModelControllerClient client;

    @BeforeClass
    public static void setup() throws UnknownHostException {
        client = ModelControllerClient.Factory.create(DomainTestSupport.masterAddress, TestSuiteEnvironment.getServerPort());
    }

    @AfterClass
    public static void afterClass() throws IOException {
        client.close();
        client = null;
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return client;
    }

    @Test
    public void testValidRootOperation() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode(null, READ_OPERATION_DESCRIPTION_OPERATION);
        op.get(NAME).set("Doesn't matter");
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testFailedRootOperation() throws IOException {
        ModelNode op = ModelUtil.createOpNode(null, READ_OPERATION_DESCRIPTION_OPERATION);
        executeInvalidOperation(op);
    }

    @Test
    public void testValidDcOperation() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("profile=default/subsystem=jmx/remoting-connector=jmx", ADD);
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testInvalidDcOperation() throws IOException {
        ModelNode op = ModelUtil.createOpNode("profile=default/subsystem=jmx/remoting-connector=jmx", ADD);
        op.get("badata").set("junk");
        executeInvalidOperation(op);
    }

    @Test
    public void testValidMasterHcOperation() throws IOException, MgmtOperationException {
        testValidHcOperation(MASTER);
    }

    @Test
    public void testValidSlaveHcOperation() throws IOException, MgmtOperationException {
        testValidHcOperation(SLAVE);
    }

    public void testValidHcOperation(String host) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("host=" + host + "/path=ff", ADD);
        op.get("path").set("/home");
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testInvalidMasterHcOperation() throws IOException {
        testInvalidHcOperation(MASTER);
    }

    @Test
    public void testInvalidSlaveHcOperation() throws IOException {
        testInvalidHcOperation(SLAVE);
    }

    private void testInvalidHcOperation(String host) throws IOException {
        ModelNode op = ModelUtil.createOpNode("host=" + host + "/path=ff", ADD);
        executeInvalidOperation(op);
    }

    @Test
    public void testValidMasterHcServerOperation() throws IOException, MgmtOperationException {
        testValidServerOperation(MASTER, MASTER_SERVER);
    }

    @Test
    public void testValidSlaveHcServerOperation() throws IOException, MgmtOperationException {
        testValidServerOperation(SLAVE, SLAVE_SERVER);
    }

    private void testValidServerOperation(String host, String server) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("host=" + host + "/server=" + server + "/subsystem=jmx/remoting-connector=jmx", ADD);
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testInvalidMasterHcServerOperation() throws IOException, MgmtOperationException {
        testInvalidServerOperation(MASTER, MASTER_SERVER);
    }

    @Test
    public void testInvalidSlaveHcServerOperation() throws IOException, MgmtOperationException {
        testInvalidServerOperation(SLAVE, SLAVE_SERVER);
    }

    private void testInvalidServerOperation(String host, String server) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("host=" + host + "/server=" + server + "/subsystem=jmx/remoting-connector=jmx", ADD);
        op.get("badata").set("junk");
        executeInvalidOperation(op);
    }

    private ModelNode createValidateOperation(ModelNode validatedOperation) throws IOException {
        ModelNode node = ModelUtil.createOpNode(null, VALIDATE_OPERATION);
        node.get(VALUE).set(validatedOperation);
        return node;
    }

    private void executeInvalidOperation(ModelNode operation) throws IOException {
        try {
            executeOperation(createValidateOperation(operation));
            Assert.fail("Should have failed on no required parameter included");
        } catch (MgmtOperationException expected) {
        }
    }
}
