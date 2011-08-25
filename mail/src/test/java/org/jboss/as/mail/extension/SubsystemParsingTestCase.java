package org.jboss.as.mail.extension;


import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemTest {
    private String SUBSYSTEM_XML =
            " <subsystem xmlns=\"urn:jboss:domain:mail:1.0\">\n" +
                    "            <mail-session jndi-name=\"java:/Mail\" >\n" +
                    "                <smtp-server address=\"localhost\" port=\"9999\">\n" +
                    "                       <login name=\"nobody\" password=\"pass\"/>\n" +
                    "                </smtp-server>\n" +
                    "                <pop3-server address=\"example.com\" port=\"1234\"/>\n" +
                    "                <imap-server address=\"example.com\" port=\"432\">\n" +
                    "                    <login name=\"nobody\" password=\"pass\"/>\n" +
                    "                </imap-server>\n" +
                    "           </mail-session>\n" +
                    "            <mail-session jndi-name=\"java:jboss/mail/Default\" >\n" +
                    "                <smtp-server address=\"localhost\" port=\"25\"/>\n" +
                    "            </mail-session>\n" +
                    "        </subsystem>";
    private static final Logger log = Logger.getLogger(SubsystemParsingTestCase.class);

    public SubsystemParsingTestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(SUBSYSTEM_XML);

        ///Check that we have the expected number of operations
        log.info("operations: " + operations);
        log.info("operations.size: " + operations.size());
        Assert.assertEquals(3, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(MailExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        KernelServices services = super.installInController(SUBSYSTEM_XML);

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(MailExtension.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller

        log.info("parseAndMarshalModel");
        KernelServices servicesA = super.installInController(SUBSYSTEM_XML);
        log.info("servicesA: " + servicesA);

        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        log.info("\n\nmodelA: " + modelA);
        String marshalled = servicesA.getPersistedSubsystemXml();
        log.info("marshaled: " + marshalled);
        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(marshalled);
        ModelNode modelB = servicesB.readWholeModel();
        log.info("\n\nmodelB: " + modelB);

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second
     * controller started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.installInController(SUBSYSTEM_XML);
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, MailExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();


        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(operations);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

    }
}
