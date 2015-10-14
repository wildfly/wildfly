package org.jboss.as.mail.extension;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
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
        log.info("operations: " + operations);
        log.info("operations.size: " + operations.size());
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
        return new Initializer();
    }


    public static class Initializer extends AdditionalInitialization {
        @Override
        protected ControllerInitializer createControllerInitializer() {
            ControllerInitializer ci = new ControllerInitializer() {

                @Override
                protected void initializeSocketBindingsOperations(List<ModelNode> ops) {

                    super.initializeSocketBindingsOperations(ops);

                    final String[] names = {"mail-imap", "mail-pop3", "mail-smtp"};
                    final int[] ports = {432, 1234, 25};
                    for (int i = 0; i < names.length; i++) {
                        final ModelNode op = new ModelNode();
                        op.get(OP).set(ADD);
                        op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                                PathElement.pathElement(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, names[i])).toModelNode());
                        op.get(HOST).set("localhost");
                        op.get(PORT).set(ports[i]);
                        ops.add(op);
                    }
                }
            };

            // Adding a socket-binding is what triggers ControllerInitializer to set up the interface
            // and socket-binding-group stuff we depend on TODO something less hacky
            ci.addSocketBinding("make-framework-happy", 59999);
            return ci;
        }

        @Override
        protected void addExtraServices(ServiceTarget target) {
            super.addExtraServices(target);
            target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();


        }
    }
}
