package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
* Test case for testing individual management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/

public class OperationsTestCase extends OperationTestCaseBase {

    // subsystem test operations
    static final ModelNode readSubsystemDefaultStackOp = getSubsystemReadOperation(ModelKeys.DEFAULT_STACK);
    static final ModelNode writeSubsystemDefaultStackOp = getSubsystemWriteOperation(ModelKeys.DEFAULT_STACK, "new-default");

    // stack test operations
    static final ModelNode addStackOp = getProtocolStackAddOperation("maximal2");
    // addStackOpWithParams calls the operation  below to check passing optional parameters
    //  /subsystem=jgroups/stack=maximal2:add(transport={type=UDP},protocols=[{type=MPING},{type=FLUSH}])
    static final ModelNode addStackOpWithParams = getProtocolStackAddOperationWithParameters("maximal2");
    static final ModelNode removeStackOp = getProtocolStackRemoveOperation("maximal2");

    // transport test operations
    static final ModelNode readTransportRackOp = getTransportReadOperation("maximal", "TCP", ModelKeys.RACK);
    static final ModelNode writeTransportRackOp = getTransportWriteOperation("maximal", "TCP", ModelKeys.RACK, "new-rack");
    static final ModelNode readTransportPropertyOp = getTransportPropertyReadOperation("maximal", "TCP", "enable_bundling");
    static final ModelNode writeTransportPropertyOp = getTransportPropertyWriteOperation("maximal", "TCP", "enable_bundling", "false");

    static final ModelNode addTransportOp = getTransportAddOperation("maximal2", "UDP");
    // addTransportOpWithProps calls the operation below to check passing optional parameters
    //   /subsystem=jgroups/stack=maximal2/transport=UDP:add(properties=[{A=>a},{B=>b}])
    static final ModelNode addTransportOpWithProps = getTransportAddOperationWithProperties("maximal2", "UDP");
    static final ModelNode removeTransportOp = getTransportRemoveOperation("maximal2", "UDP");

    // protocol test operations
    static final ModelNode readProtocolSocketBindingOp = getProtocolReadOperation("maximal", "MPING", ModelKeys.SOCKET_BINDING);
    static final ModelNode writeProtocolSocketBindingOp = getProtocolWriteOperation("maximal", "MPING", ModelKeys.SOCKET_BINDING, "new-socket-binding");
    static final ModelNode readProtocolPropertyOp = getProtocolPropertyReadOperation("maximal", "MPING", "name");
    static final ModelNode writeProtocolPropertyOp = getProtocolPropertyWriteOperation("maximal", "MPING", "name", "new-value");

    static final ModelNode addProtocolOp = getProtocolAddOperation("maximal2", "MPING");
    // addProtocolOpWithProps calls the operation below to check passing optional parameters
    //   /subsystem=jgroups/stack=maximal2:add-protocol(type=MPING, properties=[{A=>a},{B=>b}])
    static final ModelNode addProtocolOpWithProps = getProtocolAddOperationWithProperties("maximal2", "MPING");
    static final ModelNode removeProtocolOp = getProtocolRemoveOperation("maximal2", "MPING");

    /*
     * Tests access to subsystem attributes
     */
    @Test
    public void testSubsystemReadWriteOperations() throws Exception {

        KernelServices services = this.buildKernelServices();

        // read the default stack
        ModelNode result = services.executeOperation(readSubsystemDefaultStackOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("maximal", result.get(RESULT).resolve().asString());

        // write the default stack
        result = services.executeOperation(writeSubsystemDefaultStackOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),SUCCESS, result.get(OUTCOME).asString());

        // re-read the default stack
        result = services.executeOperation(readSubsystemDefaultStackOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-default", result.get(RESULT).asString());
    }

    /*
     * Tests access to transport attributes
     */
    @Test
    public void testTransportReadWriteOperation() throws Exception {

        KernelServices services = this.buildKernelServices();

        // read the transport rack attribute
        ModelNode result = services.executeOperation(readTransportRackOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("rack1", result.get(RESULT).resolve().asString());

        // write the rack attribute
        result = services.executeOperation(writeTransportRackOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the rack attribute
        result = services.executeOperation(readTransportRackOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-rack", result.get(RESULT).asString());
    }

    @Test
    public void testTransportReadWriteWithParameters() throws Exception {
        // Parse and install the XML into the controller
        KernelServices services = this.buildKernelServices();
        Assert.assertTrue("Could not create services",services.isSuccessfulBoot());
        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = services.executeOperation(addStackOpWithParams);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),SUCCESS, result.get(OUTCOME).asString());

        // write the rack attribute
        result = services.executeOperation(writeTransportRackOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the rack attribute
        result = services.executeOperation(readTransportRackOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-rack", result.get(RESULT).asString());
    }

    @Test
    public void testTransportPropertyReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

         // read the enable_bundling transport property
        ModelNode result = services.executeOperation(readTransportPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).resolve().asString());

        // write the enable_bundling transport property
        result = services.executeOperation(writeTransportPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the enable_bundling transport property
        result = services.executeOperation(readTransportPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());
    }

    @Test
    public void testProtocolReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = services.executeOperation(addStackOpWithParams);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // read the socket binding attribute
        result = services.executeOperation(readProtocolSocketBindingOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("jgroups-mping", result.get(RESULT).asString());

        // write the attribute
        result = services.executeOperation(writeProtocolSocketBindingOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the attribute
        result = services.executeOperation(readProtocolSocketBindingOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-socket-binding", result.get(RESULT).asString());
    }

    @Test
    public void testProtocolPropertyReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

         // read the name protocol property
        ModelNode result = services.executeOperation(readProtocolPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("value", result.get(RESULT).resolve().asString());

        // write the property
        result = services.executeOperation(writeProtocolPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the property
        result = services.executeOperation(readProtocolPropertyOp);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-value", result.get(RESULT).asString());
    }

}