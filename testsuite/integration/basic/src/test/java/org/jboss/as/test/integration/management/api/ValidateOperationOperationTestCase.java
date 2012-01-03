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
package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the validate-operation operation works as it should
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ValidateOperationOperationTestCase extends AbstractMgmtTestBase {


    @BeforeClass
    public static void initClient() throws Exception {
        initModelControllerClient("127.0.0.1", 9999);
    }

    @AfterClass
    public static void closeClient() throws Exception {
        closeModelControllerClient();
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
    public void testValidChildOperation() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=jmx/connector=jmx", ADD);
        op.get("registry-binding").set("REG");
        op.get("server-binding").set("SVR");
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testInvalidChildOperation() throws IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=jmx/connector=jmx", ADD);
        executeInvalidOperation(op);
    }

    @Test
    public void testValidInheritedOperation() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=jmx/connector=jmx", READ_OPERATION_DESCRIPTION_OPERATION);
        op.get(NAME).set("Doesn't matter");
        executeOperation(createValidateOperation(op));
    }

    @Test
    public void testInvalidInheritedOperation() throws IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=jmx/connector=jmx", READ_OPERATION_DESCRIPTION_OPERATION);
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
            Assert.fail("Should have failed on no required paramter included");
        } catch (MgmtOperationException expected) {
        }
    }

}
