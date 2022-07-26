/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.as.test.integration.web.access.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests various console-access-log by overriding the {@link System#out stdout} and capturing each line.
 * <p>
 * Please note there was previously a test for a false predicate which has been removed. The reason for this is when
 * testing that something is not logged we could get false positives as there could be a race condition between when
 * the log is read vs when it is written.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ConsoleAccessLogTestCase.ConsoleAccessLogSetupTask.class)
@SuppressWarnings("MagicNumber")
public class ConsoleAccessLogTestCase {

    private static final ModelNode CONSOLE_ACCESS_LOG_ADDRESS = Operations.createAddress("subsystem", "undertow",
            "server", "default-server", "host", "default-host", "setting", "console-access-log");
    private static final String[] ATTRIBUTE_NAMES = {
            "authentication-type",
            "bytes-sent",
            "date-time",
            "host-and-port",
            "local-ip",
            "local-port",
            "local-server-name",
            "query-string",
            "relative-path",
            "remote-host",
            "remote-ip",
            "remote-user",
            "request-line",
            "request-method",
            "request-path",
            "request-protocol",
            "request-scheme",
            "request-url",
            "resolved-path",
            "response-code",
            "response-reason-phrase",
            "response-time",
            "secure-exchange",
            "ssl-cipher",
            "ssl-client-cert",
            "ssl-session-id",
            "stored-response",
            "thread-name",
            "transport-protocol",
    };

    private static final int DFT_TIMEOUT = 60;

    @ArquillianResource
    private ManagementClient client;
    @ArquillianResource
    private URL url;
    private Stdout stdout;
    private PrintStream currentStdout;

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "simple-war.war")
                .addClass(SimpleServlet.class);
    }

    @Before
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void setup() {
        // Capture the current stdout to be replaced then replace stdout
        currentStdout = System.out;
        stdout = new Stdout(currentStdout);
        System.setOut(new PrintStream(stdout));
    }

    @After
    public void tearDown() throws IOException {
        // Replaced with the captured stdout
        System.setOut(currentStdout);
        executeOperation(client.getControllerClient(), Operations.createRemoveOperation(CONSOLE_ACCESS_LOG_ADDRESS), false);
    }

    @Test
    public void testDefaults() throws Exception {
        executeOperation(client.getControllerClient(), Operations.createAddOperation(CONSOLE_ACCESS_LOG_ADDRESS));
        sendRequest();
        final Collection<JsonObject> lines = findLines();
        Assert.assertFalse("Did not find eventSource in " + stdout.toString(), lines.isEmpty());
        for (JsonObject jsonObject : lines) {
            Assert.assertEquals("web-access", jsonObject.getString("eventSource"));
            Assert.assertEquals("default-host", jsonObject.getString("hostName"));
            Assert.assertEquals(HttpStatus.SC_OK, jsonObject.getInt("responseCode"));
        }
    }

    @Test
    public void testAllAttributes() throws Exception {
        final ModelNode op = Operations.createAddOperation(CONSOLE_ACCESS_LOG_ADDRESS);
        final ModelNode attributes = op.get("attributes");
        for (String name : ATTRIBUTE_NAMES) {
            attributes.get(name).setEmptyObject();
        }
        // Attributes with required parameters
        attributes.get("path-parameter").setEmptyObject().get("names").add("testPathParameter");
        attributes.get("predicate").setEmptyObject().get("names").add("testPredicate");
        attributes.get("query-parameter").setEmptyObject().get("names").add("testQueryParameter");
        attributes.get("request-header").setEmptyObject().get("names").add("User-Agent");
        attributes.get("response-header").setEmptyObject().get("names").add("Content-Type");

        executeOperation(client.getControllerClient(), op);
        sendRequest();
        final Collection<JsonObject> lines = findLines();
        Assert.assertFalse("Did not find eventSource in " + stdout.toString(), lines.isEmpty());
        for (JsonObject jsonObject : lines) {
            // First assert all keys are there
            for (String name : ATTRIBUTE_NAMES) {
                Assert.assertNotNull("Missing key " + name, jsonObject.get(translateToKey(name)));
            }
            Assert.assertNotNull("Missing key testPathParameter", jsonObject.get("testPathParameter"));
            Assert.assertNotNull("Missing key testPredicate", jsonObject.get("testPredicate"));
            Assert.assertNotNull("Missing key testQueryParameter", jsonObject.get("testQueryParameter"));
            Assert.assertNotNull("Missing key User-Agent", jsonObject.get("User-Agent"));
            Assert.assertNotNull("Missing key Content-Type", jsonObject.get("Content-Type"));

            // Assert known values
            Assert.assertEquals("web-access", jsonObject.getString("eventSource"));
            Assert.assertEquals("default-host", jsonObject.getString("hostName"));
            Assert.assertEquals("GET", jsonObject.getString("requestMethod"));
            Assert.assertEquals(url.getProtocol(), jsonObject.getString("requestScheme"));
            Assert.assertEquals("/simple-war/simple", jsonObject.getString("requestUrl"));
            Assert.assertEquals("/simple-war", jsonObject.getString("resolvedPath"));
            Assert.assertEquals(HttpStatus.SC_OK, jsonObject.getInt("responseCode"));
            Assert.assertEquals(url.getPort(), jsonObject.getInt("localPort"));
            Assert.assertEquals("/simple", jsonObject.getString("relativePath"));
            Assert.assertEquals("OK", jsonObject.getString("responseReasonPhrase"));
            Assert.assertTrue(jsonObject.getString("Content-Type").startsWith("application/json"));
        }
    }

    @Test
    public void testKeyOverrides() throws Exception {
        final ModelNode op = Operations.createAddOperation(CONSOLE_ACCESS_LOG_ADDRESS);
        final ModelNode attributes = op.get("attributes");
        final Collection<String> keys = new ArrayList<>();
        for (String name : ATTRIBUTE_NAMES) {
            final String key = reformatKeyOverride(name);
            keys.add(key);
            attributes.get(name).setEmptyObject().get("key").set(key);
        }

        executeOperation(client.getControllerClient(), op);
        sendRequest();
        final Collection<JsonObject> lines = findLines();
        Assert.assertFalse("Did not find eventSource in " + stdout.toString(), lines.isEmpty());
        for (JsonObject jsonObject : lines) {
            // First assert all keys are there
            for (String key : keys) {
                Assert.assertNotNull("Missing key " + key, jsonObject.get(translateToKey(key)));
            }
        }
    }

    @Test
    public void testOverrides() throws Exception {
        final String dateFormat = "yyyy-MM-dd'T'HH:mm:ssSSS";
        final ModelNode op = Operations.createAddOperation(CONSOLE_ACCESS_LOG_ADDRESS);
        op.get("include-host-name").set(false);
        op.get("metadata").add("@version", "1");
        final ModelNode attributes = op.get("attributes");
        final ModelNode dateTime = attributes.get("date-time").setEmptyObject();
        dateTime.get("date-format").set(dateFormat);
        dateTime.get("key").set("@timestamp");
        dateTime.get("time-zone").set("GMT");

        attributes.get("local-port").setEmptyObject().get("key").set("port");
        attributes.get("response-code").setEmptyObject().get("key").set("http_response_code");

        final ModelNode responseHeader = attributes.get("response-header").setEmptyObject();
        responseHeader.get("key-prefix").set("response_header_");
        final ModelNode names = responseHeader.get("names").setEmptyList();
        names.add("Content-Type");
        names.add("Content-Encoding");

        attributes.get("request-method").setEmptyObject().get("key").set("request_method");
        attributes.get("request-scheme").setEmptyObject().get("key").set("request_scheme");
        attributes.get("request-url").setEmptyObject().get("key").set("request_url");

        final ModelNode queryString = attributes.get("query-string").setEmptyObject();
        queryString.get("key").set("query_string");
        queryString.get("include-question-mark").set(true);

        executeOperation(client.getControllerClient(), op);

        sendRequest(new BasicNameValuePair("testParam", "testValue"));
        final Collection<JsonObject> lines = findLines();
        Assert.assertFalse("Did not find eventSource in " + stdout.toString(), lines.isEmpty());
        for (JsonObject jsonObject : lines) {
            Assert.assertEquals("web-access", jsonObject.getString("eventSource"));
            Assert.assertNull("include-host-name attribute was set to false and should not be included in the output.", jsonObject.get("hostName"));
            Assert.assertEquals("Expected @version to be 1", "1", jsonObject.getString("@version"));
            final String timestamp = jsonObject.getString("@timestamp");
            try {
                DateTimeFormatter.ofPattern(dateFormat).parse(timestamp);
            } catch (DateTimeParseException e) {
                Assert.fail(String.format("Failed to parse date %s with pattern %s: %s", timestamp, dateFormat, e.getMessage()));
            }
            Assert.assertEquals("?testParam=testValue", jsonObject.getString("query_string"));
            Assert.assertEquals("GET", jsonObject.getString("request_method"));
            Assert.assertEquals(url.getProtocol(), jsonObject.getString("request_scheme"));
            Assert.assertEquals("/simple-war/simple", jsonObject.getString("request_url"));
            Assert.assertEquals(HttpStatus.SC_OK, jsonObject.getInt("http_response_code"));
            Assert.assertEquals(url.getPort(), jsonObject.getInt("port"));
            Assert.assertTrue(jsonObject.getString("response_header_Content-Type").startsWith("application/json"));
            Assert.assertNotNull(jsonObject.get("response_header_Content-Encoding"));
        }
    }

    private void sendRequest(final NameValuePair... params) throws IOException, URISyntaxException {
        final URI uri = new URI(url.toString() + "simple");
        final URIBuilder builder = new URIBuilder(uri);
        if (params != null && params.length > 0) {
            builder.setParameters(params);
        }

        final HttpGet request = new HttpGet(builder.build());
        try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                CloseableHttpResponse response = httpClient.execute(request)
        ) {
            Assert.assertEquals("Failed to access " + uri, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        }
    }

    private Collection<JsonObject> findLines() throws InterruptedException {
        // Note this could be a potential spot for a race in the test validation. The console-access-log is
        // asynchronous so we need to wait to ensure it's fully written to the console.
        final Collection<JsonObject> result = new ArrayList<>();
        int counter = 0;
        int timeout = TimeoutUtil.adjust(DFT_TIMEOUT) * 1000;
        final long sleep = 100L;
        while (timeout > 0) {
            long before = System.currentTimeMillis();
            final String[] lines = stdout.getLines(counter);
            counter = lines.length;
            for (String line : lines) {
                if (!line.isEmpty()) {
                    try (JsonReader reader = Json.createReader(new StringReader(line))) {
                        final JsonObject jsonObject = reader.readObject();
                        if (jsonObject.get("eventSource") != null) {
                            result.add(jsonObject);
                        }
                    }
                }
            }
            if (!result.isEmpty()) {
                break;
            }
            timeout -= (System.currentTimeMillis() - before);
            TimeUnit.MILLISECONDS.sleep(sleep);
            timeout -= sleep;
        }
        return result;
    }

    private static ModelNode executeOperation(final ModelControllerClient client, final ModelNode op) throws IOException {
        return executeOperation(client, op, true);
    }

    private static ModelNode executeOperation(final ModelControllerClient client, final ModelNode op, final boolean failOnError) throws IOException {
        return executeOperation(client, Operation.Factory.create(op), failOnError);
    }

    private static ModelNode executeOperation(final ModelControllerClient client, final Operation op, final boolean failOnError) throws IOException {
        final ModelNode result = client.execute(op);
        if (failOnError && !Operations.isSuccessfulOutcome(result)) {
            Assert.fail(String.format("Failed to execute operation: %s%n%s", op, Operations.getFailureDescription(result).asString()));
        }
        return Operations.readResult(result);
    }

    private static String translateToKey(final String attributeName) {
        final StringBuilder result = new StringBuilder(attributeName.length());
        boolean toUpper = false;
        for (char c : attributeName.toCharArray()) {
            if (c == '-') {
                toUpper = true;
            } else {
                if (toUpper) {
                    result.append(Character.toUpperCase(c));
                    toUpper = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    private static String reformatKeyOverride(final String key) {
        return key.replace('-', '_');
    }

    public static class ConsoleAccessLogSetupTask implements ServerSetupTask {
        private final ModelNode formatterAddress = Operations.createAddress("subsystem", "logging", "json-formatter", "json");
        private final ModelNode consoleHandlerAddress = Operations.createAddress("subsystem", "logging", "console-handler", "CONSOLE");

        private ModelNode currentFormatter;

        @Override
        public void setup(final ManagementClient managementClient, final String s) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();

            // Get the current console handler formatter name
            currentFormatter = executeOperation(client, Operations.createReadAttributeOperation(consoleHandlerAddress, "named-formatter"));

            final CompositeOperationBuilder builder = CompositeOperationBuilder.create()
                    .addStep(Operations.createAddOperation(formatterAddress))
                    // Change the current formatter just in case a message is logged so the line will still be valid JSON
                    .addStep(Operations.createWriteAttributeOperation(consoleHandlerAddress, "named-formatter", "json"));
            executeOperation(client, builder.build(), true);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String s) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();

            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

            // Reset the named-formatter on the console
            if (currentFormatter != null) {
                builder.addStep(Operations.createWriteAttributeOperation(consoleHandlerAddress, "named-formatter", currentFormatter));
            } else {
                builder.addStep(Operations.createUndefineAttributeOperation(consoleHandlerAddress, "named-formatter"));
            }
            builder.addStep(Operations.createRemoveOperation(formatterAddress));
            executeOperation(client, builder.build(), true);
        }
    }

    private static class Stdout extends OutputStream {
        private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
        private static final String[] EMPTY = new String[0];

        private final OutputStream dftStdout;
        private byte[] buffer;
        private int bufferLen;
        private String[] lines;
        private int lineLen;

        private Stdout(final OutputStream dftStdout) {
            this.dftStdout = dftStdout;
            buffer = new byte[1024];
            lines = new String[20];
        }

        @Override
        public synchronized void write(final int b) throws IOException {
            append(b);
            dftStdout.write(b);
        }

        @Override
        public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
            // Check the array of a new line
            for (int i = off; i < len; i++) {
                append(b[i]);
            }
            dftStdout.write(b, off, len);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            write(b, 0, b.length);
            dftStdout.write(b);
        }

        @Override
        public void flush() throws IOException {
            dftStdout.flush();
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            final Iterator<String> iter = Arrays.asList(getLines()).iterator();
            while (iter.hasNext()) {
                result.append(iter.next());
                if (iter.hasNext()) {
                    result.append(System.lineSeparator());
                }
            }
            return result.toString();
        }

        @SuppressWarnings("StatementWithEmptyBody")
        private void append(final int b) {
            if (b == '\n') {
                ensureLineCapacity(lineLen + 1);
                lines[lineLen++] = new String(buffer, 0, bufferLen, StandardCharsets.UTF_8);
                bufferLen = 0;
            } else if (b == '\r') {
                // For out purposes just ignore this character
            } else {
                ensureBufferCapacity(bufferLen + 1);
                buffer[bufferLen++] = (byte) b;
            }
        }

        private void ensureBufferCapacity(final int minCapacity) {
            if (minCapacity - buffer.length > 0)
                growBuffer(minCapacity);
        }

        private void growBuffer(final int minCapacity) {
            final int oldCapacity = buffer.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                newCapacity = (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
            }
            buffer = Arrays.copyOf(buffer, newCapacity);
        }

        private void ensureLineCapacity(final int minCapacity) {
            if (minCapacity - lines.length > 0)
                growLine(minCapacity);
        }

        private void growLine(final int minCapacity) {
            final int oldCapacity = lines.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                newCapacity = (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
            }
            lines = Arrays.copyOf(lines, newCapacity);
        }

        synchronized String[] getLines() {
            if (lineLen == 0) {
                return EMPTY;
            }
            return Arrays.copyOf(lines, lineLen);
        }

        synchronized String[] getLines(final int offset) {
            if (lineLen == 0) {
                return EMPTY;
            }
            return Arrays.copyOfRange(lines, offset, lineLen);
        }
    }
}
