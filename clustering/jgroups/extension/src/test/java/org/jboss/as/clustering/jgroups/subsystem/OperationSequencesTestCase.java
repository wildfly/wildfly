/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
* Test case for testing sequences of management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationSequencesTestCase extends OperationTestCaseBase {

    // stack test operations
    static final ModelNode addStackOp = getProtocolStackAddOperation("maximal2");
    // addStackOpWithParams calls the operation  below to check passing optional parameters
    //  /subsystem=jgroups/stack=maximal2:add(transport={type=UDP},protocols=[{type=MPING},{type=FLUSH}])
    static final ModelNode addStackOpWithParams = getProtocolStackAddOperationWithParameters("maximal2");
    static final ModelNode removeStackOp = getProtocolStackRemoveOperation("maximal2");

    // transport test operations
    static final ModelNode addTransportOp = getTransportAddOperation("maximal2", "UDP");
    // addTransportOpWithProps calls the operation below to check passing optional parameters
    //   /subsystem=jgroups/stack=maximal2/transport=UDP:add(properties=[{A=>a},{B=>b}])
    static final ModelNode addTransportOpWithProps = getTransportAddOperationWithProperties("maximal2", "UDP");
    static final ModelNode removeTransportOp = getTransportRemoveOperation("maximal2", "UDP");

    // protocol test operations
    static final ModelNode addProtocolOp = getProtocolAddOperation("maximal2", "PING");
    // addProtocolOpWithProps calls the operation below to check passing optional parameters
    //   /subsystem=jgroups/stack=maximal2:add-protocol(type=MPING, properties=[{A=>a},{B=>b}])
    static final ModelNode addProtocolOpWithProps = getProtocolAddOperationWithProperties("maximal2", "PING");
    static final ModelNode removeProtocolOp = getProtocolRemoveOperation("maximal2", "PING");

    @Test
    public void testProtocolStackAddRemoveAddSequence() throws Exception {

        KernelServices services = buildKernelServices();

        ModelNode operation = Operations.createCompositeOperation(addStackOp, addTransportOp, addProtocolOp);

        // add a protocol stack, its transport and a protocol as a batch
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the stack
        result = services.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add the same stack
        result = services.executeOperation(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testProtocolStackRemoveRemoveSequence() throws Exception {

        KernelServices services = buildKernelServices();

        ModelNode operation = Operations.createCompositeOperation(addStackOp, addTransportOp, addProtocolOp);

        // add a protocol stack
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack
        result = services.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack again
        result = services.executeOperation(removeStackOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    /**
     * Tests the ability of the /subsystem=jgroups/stack=X:add() operation
     * to correctly process the optional TRANSPORT and PROTOCOLS parameters.
     */
    @Test
    public void testProtocolStackAddRemoveSequenceWithParameters() throws Exception {

        KernelServices services = buildKernelServices();

        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = services.executeOperation(addStackOpWithParams);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // check some random values

        // remove the protocol stack
        result = services.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack again
        result = services.executeOperation(removeStackOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    /**
     * Test for https://issues.jboss.org/browse/WFLY-5290 where server/test hangs when using legacy TRANSPORT alias:
     *
     * Create a simple stack, then remove, re-add a different transport, remove twice expecting the 2nd remove to fail.
     * Tests both situations when stack in inferred from :add operation and when its inferred from the existing resource.
     */
    @Test
    public void testLegacyTransportAliasSequence() throws Exception {

        KernelServices services = buildKernelServices();

        String stackName = "legacyStack";

        // add a sample stack to test legacy paths on
        ModelNode result = services.executeOperation(getProtocolStackAddOperationWithParameters(stackName));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add a thread pool
        result = services.executeOperation(getLegacyThreadPoolAddOperation(stackName, "default"));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        ModelNode op = getLegacyThreadPoolAddOperation(stackName, "default");
        op.get("operation").set("write-attribute");
        op.get("name").set("keepalive-time");
        op.get("value").set(999);
        result = services.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        op = Operations.createReadResourceOperation(getSubsystemAddress());
        op.get(ModelDescriptionConstants.INCLUDE_ALIASES).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        result = services.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, getSubsystemAddress());
        op.get(ModelDescriptionConstants.INCLUDE_ALIASES).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        result = services.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getLegacyTransportRemoveOperation(stackName));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getLegacyTransportAddOperation(stackName, "TCP"));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getLegacyTransportRemoveOperation(stackName));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getLegacyTransportRemoveOperation(stackName));
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }
}