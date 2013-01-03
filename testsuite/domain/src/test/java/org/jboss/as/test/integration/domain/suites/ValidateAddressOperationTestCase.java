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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that the validate-address operation works as it should
 *
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 */
public class ValidateAddressOperationTestCase  {

    private static DomainTestSupport testSupport;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ValidateAddressOperationTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testValidRootAddress() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode(null, ValidateAddressOperationHandler.OPERATION_NAME);
        op.get(VALUE).setEmptyList();
        final ModelNode result = executeOperation(op);
        assertTrue(result.hasDefined(VALID));
        final ModelNode value = result.get(VALID);
        assertTrue(value.asBoolean());
        assertFalse(result.hasDefined(PROBLEM));
    }

    @Test
    public void testValidPath() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode(null, ValidateAddressOperationHandler.OPERATION_NAME);
        final ModelNode addr = op.get(VALUE);
        addr.add("socket-binding-group", "standard-sockets");
        addr.add("socket-binding", "http");
        final ModelNode result = executeOperation(op);
        assertTrue(result.hasDefined(VALID));
        final ModelNode value = result.get(VALID);
        assertTrue(value.asBoolean());
        assertFalse(result.hasDefined(PROBLEM));
    }

    @Test
    public void testInvalidPath() throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode(null, ValidateAddressOperationHandler.OPERATION_NAME);
        final ModelNode addr = op.get(VALUE);
        addr.add("socket-binding-group", "standard-sockets");
        addr.add("wrong", "illegal");
        final ModelNode result = executeOperation(op);
        assertTrue(result.hasDefined(VALID));
        final ModelNode value = result.get(VALID);
        assertFalse(value.asBoolean());
        assertTrue(result.hasDefined(PROBLEM));
        final ModelNode problem = result.get(PROBLEM);
        assertTrue(problem.asString().contains("JBAS014808: Child resource '\"wrong\" => \"illegal\"' not found"));
    }

    @Test
    public void testRemote() throws Exception {
        ModelNode op = ModelUtil.createOpNode(null, ValidateAddressOperationHandler.OPERATION_NAME);
        final ModelNode addr = op.get(VALUE);
        addr.add("host", "slave");
        assertTrue(executeOperation(op).get(VALID).asBoolean());

        addr.add("server", "main-three");
        assertTrue(executeOperation(op).get(VALID).asBoolean());

        addr.add("core-service", "platform-mbean");
        addr.add("type", "garbage-collector");
        assertTrue(executeOperation(op).get(VALID).asBoolean());

        addr.add("non-existent", "resource");
        assertFalse(executeOperation(op).get(VALID).asBoolean());
    }

    private ModelNode executeOperation(final ModelNode op) throws IOException, MgmtOperationException {
        return DomainTestUtils.executeForResult(op, testSupport.getDomainMasterLifecycleUtil().getDomainClient());
    }

}
