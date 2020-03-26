/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.logging.config;

import static org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractConfigTestCase {
    static final String DEFAULT_LOG_FILE = "json.log";

    @ContainerResource
    protected ManagementClient client;

    List<JsonObject> readJsonLogFileFromModel(final String logProfileName, final String logFileName) throws IOException {
        return readJsonLogFile(logProfileName, logFileName, true);
    }

    List<JsonObject> readJsonLogFile(final String logFileName) throws IOException {
        return readJsonLogFile(null, logFileName, false);
    }

    ModelNode executeOperation(final ModelNode op) throws IOException {
        return executeOperation(Operation.Factory.create(op));
    }

    ModelNode executeOperation(final Operation op) throws IOException {
        return executeOperation(client, op);
    }

    private List<JsonObject> readJsonLogFile(final String logProfileName, final String logFileName, final boolean fromModel) throws IOException {
        final List<JsonObject> lines = new ArrayList<>();
        try (
                BufferedReader reader = fromModel ? readLogFileFromModel(logProfileName, logFileName) : readLogFile(logFileName)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                try (JsonReader jsonReader = Json.createReader(new StringReader(line))) {
                    lines.add(jsonReader.readObject());
                }
            }
        }
        return lines;
    }

    private BufferedReader readLogFileFromModel(final String profileName, final String logFileName) throws IOException {
        final ModelNode address = createSubsystemAddress(profileName, "log-file", logFileName);
        final ModelNode op = Operations.createReadAttributeOperation(address, "stream");
        final OperationResponse response = client.getControllerClient().executeOperation(Operation.Factory.create(op), OperationMessageHandler.logging);
        final ModelNode result = response.getResponseNode();
        if (Operations.isSuccessfulOutcome(result)) {
            final OperationResponse.StreamEntry entry = response.getInputStream(Operations.readResult(result).asString());
            if (entry == null) {
                throw new RuntimeException(String.format("Failed to find entry with UUID %s for log file %s",
                        Operations.readResult(result).asString(), logFileName));
            }
            return new BufferedReader(new InputStreamReader(entry.getStream(), StandardCharsets.UTF_8));
        }
        throw new RuntimeException(String.format("Failed to read log file %s: %s", logFileName, Operations.getFailureDescription(result).asString()));
    }

    private BufferedReader readLogFile(final String logFileName) throws IOException {
        final Path path = resolveLogDirectory().resolve(logFileName);
        Assert.assertTrue("Path " + path + " does not exist.", Files.exists(path));
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    private Path resolveLogDirectory() throws IOException {
        final ModelNode address = Operations.createAddress("path", "jboss.server.log.dir");
        final ModelNode op = Operations.createOperation("path-info", address);
        final ModelNode result = executeOperation(op);
        final Path dir = Paths.get(result.get("path", "resolved-path").asString());
        Assert.assertTrue("Log directory " + dir + " does not exist", Files.exists(dir));
        return dir;
    }

    static void performCall(final String url) throws Exception {
        HttpRequest.get(url, TimeoutUtil.adjust(10), TimeUnit.SECONDS);
    }

    static void assertUnexpectedLogs(final Collection<JsonObject> unexpectedLogs, final String logFile) {
        if (!unexpectedLogs.isEmpty()) {
            final StringBuilder msg = new StringBuilder("Found unexpected log messages in file ")
                    .append(logFile)
                    .append(':');
            appendLines(unexpectedLogs, msg);
            Assert.fail(msg.toString());
        }
    }

    static void assertLength(final Collection<JsonObject> lines, final int len, final String logFile) {
        if (len != lines.size()) {
            final StringBuilder msg = new StringBuilder("Found ")
                    .append(lines.size())
                    .append(" lines expected ")
                    .append(len)
                    .append(" in file ")
                    .append(logFile)
                    .append(':');
            appendLines(lines, msg);
            Assert.fail(msg.toString());
        }
    }

    static void appendLines(final Collection<JsonObject> lines, final StringBuilder msg) {
        for (JsonObject logMsg : lines) {
            msg.append(System.lineSeparator())
                    .append('\t')
                    .append(logMsg.getString("level")).append(' ')
                    .append('[').append(logMsg.getString("threadName")).append("] ")
                    .append('[').append(logMsg.getString("loggerName")).append("] ")
                    .append(logMsg.getString("message"));
        }
    }

    static Asset createLoggingConfiguration(final String fileName) throws IOException {
        final Properties properties = new Properties();

        // Configure the root logger
        properties.setProperty("logger.level", "INFO");
        properties.setProperty("logger.handlers", fileName);

        // Configure the handler
        properties.setProperty("handler." + fileName, "org.jboss.logmanager.handlers.FileHandler");
        properties.setProperty("handler." + fileName + ".level", "ALL");
        properties.setProperty("handler." + fileName + ".formatter", "json");
        properties.setProperty("handler." + fileName + ".properties", "append,autoFlush,fileName");
        properties.setProperty("handler." + fileName + ".append", "false");
        properties.setProperty("handler." + fileName + ".autoFlush", "true");
        properties.setProperty("handler." + fileName + ".fileName", "${jboss.server.log.dir}" + File.separatorChar + fileName);

        // Add the JSON formatter
        properties.setProperty("formatter.json", "org.jboss.logmanager.formatters.JsonFormatter");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        properties.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), null);
        return new ByteArrayAsset(out.toByteArray());
    }

    static ModelNode executeOperation(final ManagementClient client, final Operation op) throws IOException {
        final ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail(Operations.getFailureDescription(result).toString());
        }
        // Reload if required
        if (result.hasDefined(ClientConstants.RESPONSE_HEADERS)) {
            final ModelNode responseHeaders = result.get(ClientConstants.RESPONSE_HEADERS);
            if (responseHeaders.hasDefined("process-state")) {
                if (ClientConstants.CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED.equals(responseHeaders.get("process-state").asString())) {
                    ServerReload.executeReloadAndWaitForCompletion(client);
                } else if (ClientConstants.CONTROLLER_PROCESS_STATE_RESTART_REQUIRED.equalsIgnoreCase(responseHeaders.get("process-state").asString())) {
                    Assert.fail("Tests that require a restart need to be manual mode tests.");
                }
            }
        }
        return Operations.readResult(result);
    }

    static ModelNode createSubsystemAddress(final String profileName, final String... parts) {
        final Collection<String> address = new ArrayList<>();
        address.add("subsystem");
        address.add("logging");
        if (profileName != null) {
            address.add("logging-profile");
            address.add(profileName);
        }
        Collections.addAll(address, parts);
        return Operations.createAddress(address);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    static class UrlBuilder {
        private final URL url;
        private final String[] paths;
        private final Map<String, String> params;

        private UrlBuilder(final URL url, final String... paths) {
            this.url = url;
            this.paths = paths;
            params = new HashMap<>();
        }

        static UrlBuilder of(final URL url, final String... paths) {
            return new UrlBuilder(url, paths);
        }

        UrlBuilder addParameter(final String key, final String value) {
            params.put(key, value);
            return this;
        }

        String build() throws UnsupportedEncodingException {
            final StringBuilder result = new StringBuilder(url.toExternalForm());
            if (paths != null) {
                for (String path : paths) {
                    result.append('/').append(path);
                }
            }
            boolean isFirst = true;
            for (String key : params.keySet()) {
                if (isFirst) {
                    result.append('?');
                } else {
                    result.append('&');
                }
                final String value = params.get(key);
                result.append(URLEncoder.encode(key, "UTF-8")).append('=').append(URLEncoder.encode(value, "UTF-8"));
                isFirst = false;
            }
            return result.toString();
        }
    }

    public static class LogFileServerSetupTask extends SnapshotRestoreSetupTask {

        private static final String DEFAULT_HANDLER_NAME = "json-file";
        private static final String JSON_FORMATTER_NAME = "json";

        @Override
        protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
            executeOperation(client, createBuilder().build());
        }

        protected CompositeOperationBuilder createBuilder() {
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
            addDefaults(builder, null, DEFAULT_LOG_FILE);
            return builder;
        }

        static void addDefaults(final CompositeOperationBuilder builder, final String profileName, final String logFileName) {

            // Create a JSON formatter on the default log context
            final ModelNode address = createSubsystemAddress(profileName, "json-formatter", JSON_FORMATTER_NAME);
            builder.addStep(Operations.createAddOperation(address));

            // Add a new file handler to write JSON logs to
            ModelNode op = createFileAddOp(profileName, logFileName);
            builder.addStep(op);

            // Create new loggers for each logger we want and add the handler
            op = Operations.createAddOperation(createSubsystemAddress(profileName, "logger", LoggingServlet.LOGGER_NAME));
            op.get("handlers").setEmptyList().add(DEFAULT_HANDLER_NAME);
            builder.addStep(op);
            op = Operations.createAddOperation(createSubsystemAddress(profileName, "logger", LoggerResource.LOGGER_NAME));
            op.get("handlers").setEmptyList().add(DEFAULT_HANDLER_NAME);
            builder.addStep(op);
            op = Operations.createAddOperation(createSubsystemAddress(profileName, "logger", LoggingStartup.LOGGER_NAME));
            op.get("handlers").setEmptyList().add(DEFAULT_HANDLER_NAME);
            builder.addStep(op);
        }

        static ModelNode createFileAddOp(final String profileName, final String fileName) {
            // add file handler
            final ModelNode op = Operations.createAddOperation(createSubsystemAddress(profileName, "file-handler", DEFAULT_HANDLER_NAME));
            op.get("level").set("INFO");
            op.get("append").set(false);
            op.get("autoflush").set(true);
            final ModelNode file = new ModelNode();
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(fileName);
            op.get("file").set(file);
            op.get("named-formatter").set(JSON_FORMATTER_NAME);
            return op;
        }
    }
}
