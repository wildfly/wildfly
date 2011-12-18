package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;

/**
* Subsystem parsing test case
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
@Ignore
public class SubsystemParsingTestCase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = "subsystem-infinispan.xml" ;

    public SubsystemParsingTestCase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }
    /**
      * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {

        //Parse the subsystem xml into operations
        String subsystemXml = getSubsystemXml() ;
        List<ModelNode> operations = super.parse(subsystemXml);

        // Check that we have the expected number of operations
        // one operation for adding subsystem; one operation for adding deployment-type
        Assert.assertEquals(8, operations.size());

        //Check that each operation has the correct content
        for (int i = 0; i < 8; i++) {
            ModelNode operation = operations.get(i) ;
            System.out.println(operation);
        }
    }

    @Test
    public void testInstallIntoController() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices services = super.installInController(subsystemXml) ;

        // print out the resulting model
        ModelNode model = services.readWholeModel() ;
        System.out.println(model);

        // use some assertions here to check the correctness of the model
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(InfinispanExtension.SUBSYSTEM_NAME));

        assertRemoveSubsystemResources(services);

    }

    @Test
    public void testParseAndMarshallModel() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        // list the names of the services which have been installed
        System.out.println("service names = " + servicesA.getContainer().getServiceNames());

        ModelNode modelA = servicesA.readWholeModel() ;
        // print out the resulting model
        String marshalled = servicesA.getPersistedSubsystemXml();
        System.out.println("marshalled XML = " + marshalled);

        // install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(marshalled) ;
        ModelNode modelB = servicesB.readWholeModel() ;

        // make sure the models are identical
        super.compare(modelA, modelB);
    }

    @Test
    @Ignore
    public void testExecuteOperations() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        // list the names of the services which have been installed
        System.out.println("service names = " + servicesA.getContainer().getServiceNames());

        // test an operation
        PathAddress rpcManagerAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container","maximal"),
                PathElement.pathElement("distributed-cache", "dist"),
                PathElement.pathElement("component", "rpc-manager"));

        ModelNode readAttributeOp = new ModelNode() ;
        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readAttributeOp.get(OP_ADDR).set(rpcManagerAddr.toModelNode());
        readAttributeOp.get(NAME).set("replication-count");

        ModelNode result = servicesA.executeOperation(readAttributeOp);
        // Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        System.out.println("result = " + result.toString());

    }



    private String getSubsystemXml() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(SUBSYSTEM_XML_FILE);
        if (url == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", SUBSYSTEM_XML_FILE));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            StringWriter writer = new StringWriter();
            try {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return writer.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}