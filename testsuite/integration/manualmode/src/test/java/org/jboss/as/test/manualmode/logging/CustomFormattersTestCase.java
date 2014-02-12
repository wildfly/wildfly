package org.jboss.as.test.manualmode.logging;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomFormattersTestCase extends AbstractOperationsTestCase {

    public static final String CONTAINER = "default-jbossas";
    public static final String DEPLOYMENT = "logging-deployment";
    private static final String CUSTOM_FORMATTER_NAME = "customFormatter";
    private static final String FILE_NAME = "cf-log.xml";
    private static final String HANDLER_NAME = "xmlFile";
    private static ModelNode CUSTOM_FORMATTER_ADDRESS = new ModelNode().setEmptyList();
    private static ModelNode HANDLER_ADDRESS = new ModelNode().setEmptyList();
    private static ModelNode ROOT_LOGGER_ADDRESS = new ModelNode().setEmptyList();

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        archive.addClasses(LoggingServlet.class);
        return archive;
    }

    @BeforeClass
    public static void setupAddresses() {
        CUSTOM_FORMATTER_ADDRESS = new ModelNode().setEmptyList();
        CUSTOM_FORMATTER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging")
                .add("custom-formatter", CUSTOM_FORMATTER_NAME);

        HANDLER_ADDRESS = new ModelNode().setEmptyList();
        HANDLER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging")
                .add("file-handler", HANDLER_NAME);

        ROOT_LOGGER_ADDRESS = new ModelNode().setEmptyList();
        ROOT_LOGGER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging")
                .add("root-logger", "ROOT");
    }

    @Before
    public void startContainer() throws Exception {
        // Start the server
        container.start(CONTAINER);
        final ManagementClient managementClient = createManagementClient();
        final ModelControllerClient client = managementClient.getControllerClient();
        try {
            Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());

            // Deploy the servlet
            deployer.deploy(DEPLOYMENT);
        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
    }

    @After
    public void stopContainer() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        try {
            // Remove the servlet
            deployer.undeploy(DEPLOYMENT);
            // Stop the container
            container.stop(CONTAINER);
        } finally {
            safeClose(client);
        }
    }

    @Test
    public void testOperations() throws Exception {
        final ManagementClient managementClient = createManagementClient();
        final ModelControllerClient client = managementClient.getControllerClient();
        try {

            // Create the custom formatter
            ModelNode op = Operations.createAddOperation(CUSTOM_FORMATTER_ADDRESS);
            op.get("class").set("org.jboss.logmanager.formatters.PatternFormatter");
            op.get("module").set("org.jboss.logmanager");
            executeOperation(client, op);

            // Write some properties
            final ModelNode properties = new ModelNode().setEmptyList();
            properties.add("pattern", "%s%E%n");
            testWrite(client, CUSTOM_FORMATTER_ADDRESS, "properties", properties);

            // Undefine the properties
            testUndefine(client, CUSTOM_FORMATTER_ADDRESS, "properties");

            // Write a new class attribute, should leave in restart state
            ModelNode result = testWrite(client, CUSTOM_FORMATTER_ADDRESS, "class", "java.util.logging.XMLFormatter");
            // Check the state
            Assert.assertTrue(result.get("response-headers").get("operation-requires-reload").asBoolean());

            // Undefining the class should fail
            testUndefine(client, CUSTOM_FORMATTER_ADDRESS, "class", true);

            // Restart the server
            restart(managementClient);

            // Change the module which should require a restart
            result = testWrite(client, CUSTOM_FORMATTER_ADDRESS, "module", "sun.jdk");
            // Check the state
            Assert.assertTrue(result.get("response-headers").get("operation-requires-reload").asBoolean());

            // Undefining the module should fail
            testUndefine(client, CUSTOM_FORMATTER_ADDRESS, "module", true);

            // Restart the server
            restart(managementClient);

            // Remove the custom formatter
            op = Operations.createRemoveOperation(CUSTOM_FORMATTER_ADDRESS);
            executeOperation(client, op);

            // Verify it's been removed
            verifyRemoved(client, CUSTOM_FORMATTER_ADDRESS);

        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
    }

    @Test
    public void testUsage(@ArquillianResource URL url) throws Exception {
        final ManagementClient managementClient = createManagementClient();
        final ModelControllerClient client = managementClient.getControllerClient();
        try {

            // Create the custom formatter
            ModelNode op = Operations.createAddOperation(CUSTOM_FORMATTER_ADDRESS);
            op.get("class").set("java.util.logging.XMLFormatter");
            // the module doesn't really matter since it's a JDK, so we'll just use the jboss-logmanager.
            op.get("module").set("org.jboss.logmanager");
            executeOperation(client, op);

            // Create the handler
            op = Operations.createAddOperation(HANDLER_ADDRESS);
            final ModelNode file = op.get("file");
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(FILE_NAME);
            op.get("append").set(false);
            op.get("autoflush").set(true);
            op.get("named-formatter").set(CUSTOM_FORMATTER_NAME);
            executeOperation(client, op);

            // Add the handler to the root logger
            op = Operations.createOperation("add-handler", ROOT_LOGGER_ADDRESS);
            op.get(ModelDescriptionConstants.NAME).set(HANDLER_NAME);
            executeOperation(client, op);

            // Get the log file
            op = Operations.createOperation("resolve-path", HANDLER_ADDRESS);
            ModelNode result = executeOperation(client, op);
            final Path logFile = Paths.get(readResultAsString(result));

            // The file should exist
            Assert.assertTrue("The log file was not created.", Files.exists(logFile));

            // Log 5 records
            doLog(url, "Test message: ", 5);

            // Read the log file
            try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                final Pattern pattern = Pattern.compile("^(<message>)+(Test message: \\d)+(</message>)$");
                final List<String> messages = new ArrayList<>(5);
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmedLine = line.trim();
                    final Matcher m = pattern.matcher(trimmedLine);
                    // Very simple xml parsing
                    if (m.matches()) {
                        messages.add(m.group(2));
                    }
                }

                // Should be 5 messages
                Assert.assertEquals(5, messages.size());
                // Check each message
                int count = 0;
                for (String msg : messages) {
                    Assert.assertEquals("Test message: " + count++, msg);
                }
            }

            // Remove the handler from the root-logger
            op = Operations.createOperation("remove-handler", ROOT_LOGGER_ADDRESS);
            op.get(ModelDescriptionConstants.NAME).set(HANDLER_NAME);
            executeOperation(client, op);


            // Remove the custom formatter
            op = Operations.createRemoveOperation(CUSTOM_FORMATTER_ADDRESS);
            executeOperation(client, op);

            // Remove the handler
            op = Operations.createRemoveOperation(HANDLER_ADDRESS);
            executeOperation(client, op);

            // So we don't pollute other, verify the formatter and handler have been removed
            op = Operations.createReadAttributeOperation(ROOT_LOGGER_ADDRESS, "handlers");
            result = executeOperation(client, op);
            // Should be a list type
            final List<ModelNode> handlers = Operations.readResult(result).asList();
            for (ModelNode handler : handlers) {
                Assert.assertNotEquals(CUSTOM_FORMATTER_NAME, handler.asString());
            }
            verifyRemoved(client, CUSTOM_FORMATTER_ADDRESS);
            verifyRemoved(client, HANDLER_ADDRESS);

            // Delete the log file
            Files.delete(logFile);
            // Ensure it's been deleted
            Assert.assertFalse(Files.exists(logFile));
        } finally {
            safeClose(managementClient);
            safeClose(client);
        }
    }

    private void restart(final ManagementClient managementClient) {
        container.stop(CONTAINER);
        container.start(CONTAINER);
        // Just make sure it's running
        Assert.assertTrue("Container is not started", managementClient.isServerInRunningState());
    }

    private void doLog(final URL url, final String msg, final int count) throws Exception {
        for (int i = 0; i < count; i++) {
            final String s = msg + i;
            final Response response = getResponse(new URL(url, "logger?msg=" + URLEncoder.encode(s, "utf-8")));
            Assert.assertTrue("Invalid response statusCode: " + response + " URL: " + url, response.statusCode == HttpServletResponse.SC_OK);
        }
    }

    static ManagementClient createManagementClient() {
        return new ManagementClient(TestSuiteEnvironment.getModelControllerClient(), TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
    }

    static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception ignore) {
            // ignore
        }
    }

    Response getResponse(final URL url) throws IOException {
        final HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
        return new Response(connection.getResponseCode(), connection.getResponseMessage());
    }

    static class Response {
        final int statusCode;
        final String message;

        Response(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        @Override
        public String toString() {
            return "Response: " + statusCode + " - " + message;
        }
    }
}
