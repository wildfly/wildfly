package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.List;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
* Test case for testing sequences of management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
@RunWith(BMUnitRunner.class)
public class OperationSequencesTestCase extends OperationTestCaseBase {

    // subsystem test operations
    static final ModelNode addSubsystemOp = getSubsystemAddOperation("maximal2");
    static final ModelNode removeSubsystemOp = getSubsystemRemoveOperation();

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
    static final ModelNode addProtocolOp = getProtocolAddOperation("maximal2", "MPING");
    // addProtocolOpWithProps calls the operation below to check passing optional parameters
    //   /subsystem=jgroups/stack=maximal2:add-protocol(type=MPING, properties=[{A=>a},{B=>b}])
    static final ModelNode addProtocolOpWithProps = getProtocolAddOperationWithProperties("maximal2", "MPING");
    static final ModelNode removeProtocolOp = getProtocolRemoveOperation("maximal2", "MPING");

    @Test
    public void testProtocolStackAddRemoveAddSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        // KernelServices servicesA = super.installInController(subsystemXml) ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        ModelNode[] batchToAddStack = {addStackOp, addTransportOp, addProtocolOp} ;
        ModelNode compositeOp = getCompositeOperation(batchToAddStack);

        // add a protocol stack, its transport and a protocol as a batch
        ModelNode result = servicesA.executeOperation(compositeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the stack
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add the same stack
        result = servicesA.executeOperation(compositeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testProtocolStackRemoveRemoveSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        ModelNode[] batchToAddStack = {addStackOp, addTransportOp, addProtocolOp} ;
        ModelNode compositeOp = getCompositeOperation(batchToAddStack);

        // add a protocol stack
        ModelNode result = servicesA.executeOperation(compositeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack again
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    /*
     * Tests the ability of the /subsystem=jgroups/stack=X:add() operation
     * to correctly process the optional TRANSPORT and PROTOCOLS parameters.
     */
    @Test
    public void testProtocolStackAddRemoveSequenceWithParameters() throws Exception {
        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = servicesA.executeOperation(addStackOpWithParams);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // check some random values

        // remove the protocol stack
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack again
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    @Test
    @BMRule(name="Test remove rollback operation",
            targetClass="org.jboss.as.clustering.jgroups.subsystem.StackRemoveHandler",
            targetMethod="performRuntime",
            targetLocation="AT EXIT",
            action="traceln(\"Injecting rollback fault via Byteman\");$1.setRollbackOnly()")
    public void testProtocolStackRemoveRollback() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        ModelNode[] batchToAddStack = {addStackOp, addTransportOp, addProtocolOp} ;
        ModelNode compositeOp = getCompositeOperation(batchToAddStack);

        // add a protocol stack
        ModelNode result = servicesA.executeOperation(compositeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the protocol stack
        // the remove has OperationContext.setRollbackOnly() injected
        // and so is expected to fail
        result = servicesA.executeOperation(removeStackOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());

        // need to check that all services are correctly re-installed
        ServiceName channelFactoryServiceName = ChannelFactoryService.getServiceName("maximal2");
        Assert.assertNotNull("channel factory service not installed", servicesA.getContainer().getService(channelFactoryServiceName));
    }

     private void listMSCServices(KernelServices services, String marker) {
        ServiceRegistry registry = services.getContainer() ;
        List<ServiceName> names = registry.getServiceNames() ;
        System.out.println("Services: " + marker);
        for (ServiceName name : names) {
            System.out.println("name = " + name.toString());
        }
    }

    private boolean isMSCServicePresent(KernelServices services, ServiceName serviceName) {
       ServiceRegistry registry = services.getContainer() ;
       return (registry.getService(serviceName) != null);
    }
}