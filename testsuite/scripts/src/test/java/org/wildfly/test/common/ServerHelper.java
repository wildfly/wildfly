/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.common;

import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STARTING;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STOPPING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerHelper {

    private static final Logger logger = Logger.getLogger(ServerHelper.class.getName());

    public static final ModelNode EMPTY_ADDRESS = new ModelNode().setEmptyList();
    public static final int TIMEOUT = TimeoutUtil.adjust(Integer.parseInt(System.getProperty("jboss.test.start.timeout", "15")));
    public static final Path JBOSS_HOME;
    public static final String[] DEFAULT_SERVER_JAVA_OPTS = {
            "-Djboss.management.http.port=" + TestSuiteEnvironment.getServerPort(),
            "-Djboss.bind.address.management=" + TestSuiteEnvironment.getServerAddress(),
    };

    /**
     * Strings we expect to find in input arguments. This is far from a complete list;
     * it's meant to be used as a simple smoke test. If our scripts no longer produce one
     * of these, it's fine to edit and replace it with something else.
     */
    public static final String[] DEFAULT_EXPECTED_INPUT_ARGS = {
            "--add-opens=java.base/java.lang=ALL-UNNAMED", // basic thing unlikely to be removed
            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED", // as of Aug 2026, the first modular arg
            "--add-opens=java.naming/javax.naming=ALL-UNNAMED", // as of Aug 2026 the last modular arg
            "jdk.serialFilter" // in one way or another we'll do a JEPS 290 global config with a command using this string
    };

    static {
        EMPTY_ADDRESS.protect();
        final String jbossHome = System.getProperty("jboss.home");

        if (isNullOrEmpty(jbossHome)) {
            throw new RuntimeException("Failed to configure environment. No jboss.home system property or JBOSS_HOME " +
                    "environment variable set.");
        }
        JBOSS_HOME = Paths.get(jbossHome).toAbsolutePath();
    }

    /**
     * Shuts down a standalone server.
     *
     * @throws IOException if an error occurs communicating with the server
     */
    public static void shutdownStandalone(final ModelControllerClient client) throws IOException {
        final ModelNode op = Operations.createOperation("shutdown");
        op.get("timeout").set(0);
        final ModelNode response = client.execute(op);
        if (!Operations.isSuccessfulOutcome(response)) {
            Assert.fail("Failed to shutdown server: " + Operations.getFailureDescription(response).asString());
        }
    }

    /**
     * Checks to see if a standalone server is running.
     *
     * @param client the client used to communicate with the server
     *
     * @return {@code true} if the server is running, otherwise {@code false}
     */
    public static boolean isStandaloneRunning(final ModelControllerClient client) {
        try {
            ModelNode response = client.execute(Operations.createReadAttributeOperation(EMPTY_ADDRESS, "server-state"));
            if (Operations.isSuccessfulOutcome(response)) {
                final String state = Operations.readResult(response).asString();
                if (CONTROLLER_PROCESS_STATE_STARTING.equals(state)
                        || CONTROLLER_PROCESS_STATE_STOPPING.equals(state)) {
                    logger.severe("Server server-state check failed -- " + response);
                    return false;
                }
            } else {
                logger.severe("Server status check failed -- " + response);
                return false;
            }

            return true;
        } catch (RuntimeException | IOException toLog) {
            logger.severe("Server status check failed -- " + toLog);
            return false;
        }

    }

    /**
     * Waits the given amount of time in seconds for a standalone server to start.
     * <p>
     * If the {@code process} is not {@code null} and a timeout occurs the process will be
     * {@linkplain Process#destroy() destroyed}.
     * </p>
     *
     * @param process the Java process can be {@code null} if no process is available
     *
     * @throws InterruptedException if interrupted while waiting for the server to start
     * @throws RuntimeException     if the process has died
     */
    public static void waitForStandalone(final Process process, final Supplier<String> failureDescription)
            throws InterruptedException, IOException {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            waitForStart(process, failureDescription, () -> ServerHelper.isStandaloneRunning(client));
        }
    }

    /**
     * Checks to see if a domain server is running.
     *
     * @param client the client used to communicate with the server
     *
     * @return {@code true} if the server, and it's auto-start servers, are running, otherwise {@code false}
     */
    public static boolean isDomainRunning(final ModelControllerClient client) {
        return isDomainRunning(client, false);
    }

    /**
     * Checks to see if a domain server is running.
     *
     * @param client      the client used to communicate with the server
     * @param forShutdown if this is checking for a shutdown
     *
     * @return {@code true} if the server, and it's auto-start servers, are running, otherwise {@code false}
     */
    public static boolean isDomainRunning(final ModelControllerClient client, final boolean forShutdown) {

        final DomainClient domainClient = (client instanceof DomainClient ? (DomainClient) client : DomainClient.Factory.create(client));
        try {
            // Check for admin-only
            final ModelNode hostAddress = determineHostAddress(domainClient);
            final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create()
                    .addStep(Operations.createReadAttributeOperation(hostAddress, "running-mode"))
                    .addStep(Operations.createReadAttributeOperation(hostAddress, "host-state"));

            ModelNode response = domainClient.execute(builder.build());
            if (!Operations.isSuccessfulOutcome(response)) {
                logger.severe("HostController status check failed -- " + response);
                return false;
            }

            ModelNode result = Operations.readResult(response);

            // If we're admin-only we should be fully started by now, so check we are.
            // With servers, maybe not, so we don't check
            if ("ADMIN_ONLY".equals(Operations.readResult(result.get("step-1")).asString())) {
                ModelNode step2Response = result.get("step-2");
                if (Operations.isSuccessfulOutcome(step2Response)) {  // todo wouldn't this always be true if the overall op succeeded?
                    final String state = Operations.readResult(step2Response).asString();
                    boolean started = !CONTROLLER_PROCESS_STATE_STARTING.equals(state)
                            && !CONTROLLER_PROCESS_STATE_STOPPING.equals(state);
                    if (!started) {
                        logger.severe("HostController host-state is invalid: " + state);
                    }
                    return started;
                } else {
                    logger.severe("HostController host-state check failed -- " + response);
                    return false;
                }
            }

            final Map<ServerIdentity, ServerStatus> servers = new HashMap<>();
            final Map<ServerIdentity, ServerStatus> statuses = domainClient.getServerStatuses();
            for (ServerIdentity id : statuses.keySet()) {
                final ServerStatus status = statuses.get(id);
                switch (status) {
                    case DISABLED:
                    case STARTED: {
                        servers.put(id, status);
                        break;
                    }
                }
            }
            if (forShutdown) {
                return statuses.isEmpty();
            }
            return statuses.size() == servers.size();
        } catch (IllegalStateException | IOException ignore) {
        }
        return false;
    }

    /**
     * Shuts down a domain server.
     *
     * @throws IOException if an error occurs communicating with the server
     */
    public static void shutdownDomain(final DomainClient client) throws IOException {
        // Now shutdown the host
        final ModelNode address = ServerHelper.determineHostAddress(client);
        final ModelNode shutdownOp = Operations.createOperation("shutdown", address);
        final ModelNode response = client.execute(shutdownOp);
        if (!Operations.isSuccessfulOutcome(response)) {
            Assert.fail("Failed to stop servers: " + response);
        }
    }

    /**
     * Waits the given amount of time in seconds for a domain server to start.
     * <p>
     * If the {@code process} is not {@code null} and a timeout occurs the process will be
     * {@linkplain Process#destroy() destroyed}.
     * </p>
     *
     * @param process the Java process can be {@code null} if no process is available
     *
     * @throws InterruptedException if interrupted while waiting for the server to start
     * @throws RuntimeException     if the process has died
     */
    public static void waitForDomain(final Process process, final Supplier<String> failureDescription)
            throws InterruptedException, IOException {
        try (DomainClient client = DomainClient.Factory.create(TestSuiteEnvironment.getModelControllerClient())) {
            waitForStart(process, failureDescription, () -> ServerHelper.isDomainRunning(client));
        }
    }

    public static void waitForManagedServer(final DomainClient client, final String serverName, final Supplier<String> failureDescription) throws InterruptedException {
            waitForStart(null, failureDescription, () -> {
                // Wait for the server to start
                ServerStatus serverStatus = null;
                final Map<ServerIdentity, ServerStatus> statuses = client.getServerStatuses();
                for (Map.Entry<ServerIdentity, ServerStatus> entry : statuses.entrySet()) {
                    if (serverName.equals(entry.getKey().getServerName())) {
                        serverStatus = entry.getValue();
                        break;
                    }
                }
                return serverStatus == ServerStatus.STARTED;
            });
    }

    public static List<JsonObject> readLogFileFromModel(final String logFileName, final String... addressPrefix) throws IOException {
        final Collection<String> addr = new ArrayList<>();
        if (addressPrefix != null) {
            Collections.addAll(addr, addressPrefix);
        }
        addr.add("subsystem");
        addr.add("logging");
        addr.add("log-file");
        addr.add(logFileName);
        final ModelNode address = Operations.createAddress(addr);
        final ModelNode op = Operations.createReadAttributeOperation(address, "stream");
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            final OperationResponse response = client.executeOperation(Operation.Factory.create(op), OperationMessageHandler.logging);
            final ModelNode result = response.getResponseNode();
            if (Operations.isSuccessfulOutcome(result)) {
                final OperationResponse.StreamEntry entry = response.getInputStream(Operations.readResult(result).asString());
                if (entry == null) {
                    throw new RuntimeException(String.format("Failed to find entry with UUID %s for log file %s",
                            Operations.readResult(result).asString(), logFileName));
                }
                final List<JsonObject> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entry.getStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try (JsonReader jsonReader = Json.createReader(new StringReader(line))) {
                            lines.add(jsonReader.readObject());
                        }
                    }
                }
                return lines;
            }
            throw new RuntimeException(String.format("Failed to read log file %s: %s", logFileName, Operations.getFailureDescription(result).asString()));
        }
    }

    /**
     * Attempts to determine the address for a domain server.
     *
     * @param client the client used to communicate with the server
     *
     * @return the host address
     *
     * @throws IOException if an error occurs determining the host name
     */
    public static ModelNode determineHostAddress(final ModelControllerClient client) throws IOException {
        return Operations.createAddress("host", determineHostName(client));
    }

    /**
     * Attempts to determine the name for a domain server.
     *
     * @param client the client used to communicate with the server
     *
     * @return the host name
     *
     * @throws IOException if an error occurs determining the host name
     */
    public static String determineHostName(final ModelControllerClient client) throws IOException {
        final ModelNode op = Operations.createReadAttributeOperation(EMPTY_ADDRESS, "local-host-name");
        ModelNode response = client.execute(op);
        if (Operations.isSuccessfulOutcome(response)) {
            return Operations.readResult(response).asString();
        }
        throw new IOException("Failed to determine host name: " + Operations.readResult(response).asString());
    }

    /**
     * Checks that the core-service=management resource's read-boot-errors op returns no errors.
     * @param client the client to connect to the target process.
     * @param baseAddress base address under which the core-service=management resource can be found.
     */
    public static void checkBootErrors(final ModelControllerClient client, PathAddress baseAddress) {

        final ModelNode bootErrorAddress = baseAddress.append("core-service", "management").toModelNode();
        try {
            ModelNode response = client.execute(Operations.createOperation("read-boot-errors", bootErrorAddress));
            Assert.assertTrue("Server read-boot-errors failed -- " + response,
                    Operations.isSuccessfulOutcome(response));
            final ModelNode errors = Operations.readResult(response);
            Assert.assertTrue("Server has boot errors -- " + response,
                    !errors.isDefined() || errors.asList().isEmpty());
        } catch (RuntimeException | IOException toLog) {
            Assert.fail("Server read-boot-errors failed -- " + toLog);
        }
    }

    /**
     * Checks whether the {@code input-arguments} attribute from the management API's resource for the RuntimeMXBean
     * includes all the expected strings.
     * @param client the client to connect to the target process.
     * @param baseAddress base address under which the platform mbean resource can be found.
     * @param expectedArgs the expected arguments
     */
    public static void checkInputArgs(final ModelControllerClient client, PathAddress baseAddress, String... expectedArgs) {

        final ModelNode runtimeMBeanAddress = baseAddress.append("core-service", "platform-mbean")
                .append("type", "runtime").toModelNode();
        try {
            ModelNode response = client.execute(Operations.createReadAttributeOperation(runtimeMBeanAddress, "input-arguments"));
            Assert.assertTrue("input-args check failed -- " + response, Operations.isSuccessfulOutcome(response));
            final String inputArgs = Operations.readResult(response).asString();
            for (String expectedArg : expectedArgs) {
                Assert.assertTrue("input-args do not contain " + expectedArg + " -- " + response,
                        inputArgs.contains(expectedArg));
            }
        } catch (RuntimeException | IOException e) {
            Assert.fail("input-args check failed -- " + e);
        }
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void waitForStart(final Process process, final Supplier<String> failureDescription, final BooleanSupplier check) throws InterruptedException {
        long timeout = ServerHelper.TIMEOUT * 1000;
        final long sleep = 100L;
        while (timeout > 0) {
            long before = System.currentTimeMillis();
            if (check.getAsBoolean())
                break;
            timeout -= (System.currentTimeMillis() - before);
            if (process != null && !process.isAlive()) {
                Assert.fail(failureDescription.get());
            }
            TimeUnit.MILLISECONDS.sleep(sleep);
            timeout -= sleep;
        }
        if (timeout <= 0) {
            if (process != null) {
                process.destroy();
            }
            Assert.fail(String.format("The server did not start within %s seconds: %s", ServerHelper.TIMEOUT, failureDescription.get()));
        }
    }
}
