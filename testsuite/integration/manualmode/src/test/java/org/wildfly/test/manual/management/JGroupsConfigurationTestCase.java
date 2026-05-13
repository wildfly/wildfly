package org.wildfly.test.manual.management;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * Check TLS parameters of JGroup transports
 */
@RunAsClient()
@RunWith(Arquillian.class)
@ServerControl(manual = true)
public class JGroupsConfigurationTestCase {
    public static final String DEFAULT_JBOSSAS = "default-jbossas";

    @ArquillianResource
    private static volatile ContainerController container;

    private static CLIWrapper cli = null;

    @Test
    @InSequence(-1)
    public void init() throws Exception {
        container.start(DEFAULT_JBOSSAS); // use standalone-ha
        cli = new CLIWrapper(true);
    }

    /**
     * Verify that SensitiveTargetAccessConstraintDefinition.SSL_REF RBAC constraint is set for ssl context parameters
     */
    @Test
    @InSequence(1)
    public void testAccessConstraints() throws Exception {
        try (ModelControllerClient modelControllerClient = TestSuiteEnvironment.getModelControllerClient()) {
            // /subsystem=jgroups/stack=tcp/transport=TCP:read-resource-description()
            ModelNode op = new ModelNode();
            op.get("operation").set("read-resource-description");
            ModelNode address = op.get("address");
            address.add("subsystem", "jgroups");
            address.add("stack", "tcp");
            address.add("transport", "TCP");

            // send request
            ModelNode response = modelControllerClient.execute(op);
            Assert.assertEquals("Operation failed", "success", response.get("outcome").asString());
            ModelNode attributes = response.get("result").get("attributes");

            // check ssl context attributes
            String[] attributesToCheck = {"client-ssl-context", "server-ssl-context"};
            for (String attr : attributesToCheck) {
                ModelNode accessConstraints = attributes.get(attr).get("access-constraints");
                Assert.assertTrue("Attribute " + attr + " should have access-constraints", accessConstraints.isDefined());
                ModelNode sslRef = accessConstraints.get("sensitive").get("ssl-ref");
                Assert.assertTrue("Attribute " + attr + " should have ssl-ref", sslRef.isDefined());
                Assert.assertEquals("Attribute " + attr + " should have core type constraint",
                        "core", sslRef.get("type").asString());
            }
        }
    }

    /**
     * Check that ssl context attributes are available on TCP transport
     */
    @Test
    @InSequence(2)
    public void testTcpAttributes() throws Exception {
        cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP:read-attribute(name=server-ssl-context)");
        Assert.assertThat(cli.readOutput(), containsString("undefined"));
        cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP:read-attribute(name=client-ssl-context)");
        Assert.assertThat(cli.readOutput(), containsString("undefined"));
    }

    /**
     * Check that ssl context attributes are not available on UDP transport
     */
    @Test
    @InSequence(3)
    public void testUdpAttributes() throws Exception {
        try {
            cli.sendLineForValidation("/subsystem=jgroups/stack=udp/transport=UDP:read-attribute(name=server-ssl-context)");
            Assert.fail("Exception has been expected, but it has not been thrown");
        } catch (CommandLineException e) {
            Assert.assertThat("Wrong exception", e.getMessage(), containsString("WFLYCTL0201"));
        }
        try {
            cli.sendLineForValidation("/subsystem=jgroups/stack=udp/transport=UDP:read-attribute(name=client-ssl-context)");
            Assert.fail("Exception has been expected, but it has not been thrown");
        } catch (CommandLineException e) {
            Assert.assertThat("Wrong exception", e.getMessage(), containsString("WFLYCTL0201"));
        }
    }

    /**
     * Check that ssl context attributes are available on TCP_NIO2 transport
     */
    @Test
    @InSequence(4)
    public void testTcpNio2Attributes() throws Exception {
        try {
            cli.sendLine("batch");
            cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP:remove");
            cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:add(socket-binding=jgroups-tcp)");
            cli.sendLine("run-batch");

            try {
                cli.sendLineForValidation("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:read-attribute(name=server-ssl-context)");
                Assert.fail("Exception has been expected, but it has not been thrown");
            } catch (CommandLineException e) {
                Assert.assertThat("Wrong exception", e.getMessage(), containsString("WFLYCTL0201"));
            }
            try {
                cli.sendLineForValidation("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:read-attribute(name=client-ssl-context)");
                Assert.fail("Exception has been expected, but it has not been thrown");
            } catch (CommandLineException e) {
                Assert.assertThat("Wrong exception", e.getMessage(), containsString("WFLYCTL0201"));
            }
        } finally {
            cli.sendLine("batch");
            cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:remove");
            cli.sendLine("/subsystem=jgroups/stack=tcp/transport=TCP:add(socket-binding=jgroups-tcp)");
            cli.sendLine("run-batch");
        }
    }

    @Test
    @InSequence(99)
    public void cleanup() throws Exception {
        container.stop(DEFAULT_JBOSSAS);
        cli.quit();
    }
}
