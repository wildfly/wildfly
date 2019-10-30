package org.jboss.as.mail.extension;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
public class MailSubsystem10TestCase extends AbstractSubsystemBaseTest {

    private static final Logger log = Logger.getLogger(MailSubsystem10TestCase.class);

    public MailSubsystem10TestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(getSubsystemXml());

        ///Check that we have the expected number of operations
        //log.info("operations: " + operations);
        //log.info("operations.size: " + operations.size());
        Assert.assertEquals(7, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(MailExtension.SUBSYSTEM_NAME, element.getValue());
    }


    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new MailSubsystemTestBase.DefaultInitializer();
    }


    @Override
    public void testSchema() throws Exception {
        //
    }
}
