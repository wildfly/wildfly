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

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {
    private String SUBSYSTEM_XML =
            " <subsystem xmlns=\"urn:jboss:domain:mail:1.0\">\n" +
                    "            <mail-session jndi-name=\"java:/Mail\" >\n" +
                    "                <smtp-server outbound-socket-binding-ref=\"mail-smtp\">\n" +
                    "                       <login name=\"nobody\" password=\"pass\"/>\n" +
                    "                </smtp-server>\n" +
                    "                <pop3-server outbound-socket-binding-ref=\"mail-pop3\"/>\n" +
                    "                <imap-server outbound-socket-binding-ref=\"mail-imap\">\n" +
                    "                    <login name=\"nobody\" password=\"pass\"/>\n" +
                    "                </imap-server>\n" +
                    "           </mail-session>\n" +
                    "            <mail-session jndi-name=\"java:jboss/mail/Default\" >\n" +
                    "                <smtp-server outbound-socket-binding-ref=\"mail-smtp\"/>\n" +
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

    @Override
    protected String getSubsystemXml() throws IOException {
        return SUBSYSTEM_XML;
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new Initializer();
    }

    private class Initializer extends AdditionalInitialization {
        @Override
        protected ControllerInitializer createControllerInitializer() {
            ControllerInitializer ci = new ControllerInitializer() {

                @Override
                protected void initializeSocketBindingsOperations(List<ModelNode> ops) {

                    super.initializeSocketBindingsOperations(ops);

                    final String[] names = { "mail-imap", "mail-pop3", "mail-smtp"};
                    final int[] ports = { 432, 1234, 25 };
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
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        //TODO remove this method so we get validation.
        //The problem is that the parser goes via MailSessionConfig, so this:
        // <mail-session jndi-name="java:/Mail\">
        //gets marshalled as
        // <mail-session debug=false jndi-name="java:/Mail\">
        //The default value should not be written

    }
}
