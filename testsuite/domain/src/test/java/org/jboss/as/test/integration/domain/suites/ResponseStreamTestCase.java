/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.extension.streams.LogStreamExtension;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of propagating response streams around a domain.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ResponseStreamTestCase {
    private static final Logger log = Logger.getLogger(ResponseStreamTestCase.class);

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    private static final String QUERY_PARAM = "useStreamAsResponse";
    private static final String APPLICATION_JSON = "application/json";

    /** The standard password used for test user accounts */
    public static final String STD_PASSWORD = "t3stSu!tePassword";

    private static DomainTestSupport testSupport;
    private static DomainClient masterClient;
    private static DomainClient slaveClient;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ResponseStreamTestCase.class.getSimpleName());
        masterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        slaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();
        // Initialize the test extension
        ExtensionSetup.initializeLogStreamExtension(testSupport);

        ModelNode addExtension = Util.createAddOperation(PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));

        executeForResult(addExtension, masterClient);

        ModelNode addSubsystem = Util.createAddOperation(PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME)));
        executeForResult(addSubsystem, masterClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        ModelNode removeSubsystem = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME)));
        executeForResult(removeSubsystem, masterClient);

        ModelNode removeExtension = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
        executeForResult(removeExtension, masterClient);

        testSupport = null;
        masterClient = null;
        slaveClient = null;
        DomainTestSuite.stopSupport();
    }

    private String logMessageContent;
    private HttpClient httpClient;

    @Before
    public void before() throws IOException {

        logMessageContent = String.valueOf(System.currentTimeMillis());
        ModelNode opNode = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        opNode.get(VALUE).set(logMessageContent);
        Operation op = OperationBuilder.create(opNode).build();
        masterClient.executeOperation(op, OperationMessageHandler.DISCARD);
    }

    @After
    public void after() throws IOException {
        ModelNode opNode = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        Operation op = OperationBuilder.create(opNode).build();
        masterClient.executeOperation(op, OperationMessageHandler.DISCARD);

        shutdownHttpClient();

    }

    private void shutdownHttpClient() {
        if (httpClient != null) {
            try {
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                httpClient.getConnectionManager().shutdown();
            } catch (Exception e) {
                log.error(e);
            } finally {
                httpClient = null;
            }
        }
    }

    @Test
    public void testMasterHost() throws IOException {
        PathAddress base = PathAddress.pathAddress(PROFILE, "default");
        readLogFile(createReadAttributeOp(base), masterClient, false);
    }

    @Test
    public void testSlaveHost() throws IOException {
        PathAddress base = PathAddress.pathAddress(PROFILE, "default");
        readLogFile(createReadAttributeOp(base), slaveClient, false);
    }

    @Test
    public void testMasterServer() throws IOException {
        PathAddress base = PathAddress.pathAddress(HOST, "master").append(SERVER, "main-one");
        readLogFile(createReadAttributeOp(base), masterClient, true);
        readLogFile(createOperationOp(base), masterClient, true);
    }

    @Test
    public void testSlaveServer() throws IOException {
        PathAddress base = PathAddress.pathAddress(HOST, "slave").append(SERVER, "main-three");
        readLogFile(createReadAttributeOp(base), masterClient, true);
        readLogFile(createOperationOp(base), masterClient, true);
        readLogFile(createReadAttributeOp(base), slaveClient, true);
        readLogFile(createOperationOp(base), slaveClient, true);
    }

    @Test
    public void testComposite() throws IOException {
        ModelNode composite = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        steps.add(createReadAttributeOp(PathAddress.pathAddress(PROFILE, "default")));
        steps.add(createReadAttributeOp(PathAddress.pathAddress(HOST, "master").append(SERVER, "main-one")));
        steps.add(createReadAttributeOp(PathAddress.pathAddress(HOST, "slave").append(SERVER, "main-three")));
        Operation op = OperationBuilder.create(composite).build();
        OperationResponse response = null;
        try {
            response = masterClient.executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            System.out.println(respNode.toString());
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            //Assert.assertEquals(3, streams.size());

            ModelNode result0 = respNode.get(RESULT, "step-1", RESULT);
            Assert.assertEquals(ModelType.STRING, result0.getType());
            String uuid = result0.asString();
            processResponseStream(response, uuid, false, true);

            ModelNode result1 = respNode.get(RESULT, "step-2", RESULT);
            Assert.assertEquals(ModelType.STRING, result1.getType());
            uuid = result1.asString();
            processResponseStream(response, uuid, true, false);

            ModelNode result2 = respNode.get(RESULT, "step-3", RESULT);
            Assert.assertEquals(ModelType.STRING, result2.getType());
            uuid = result2.asString();
            processResponseStream(response, uuid, true, false);

        } finally {
            StreamUtils.safeClose(response);
        }
    }

    @Test
    public void testGetWithQueryParameter() throws Exception {
        URL url = buildURL(true, true, null);
        HttpGet httpget = new HttpGet(url.toURI());
        HttpResponse response = getHttpClient(url).execute(httpget);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("text/plain"));
    }

    @Test
    public void testGetWithSpecifiedQueryParameter() throws Exception {
        URL url = buildURL(true, true, 0);
        HttpGet httpget = new HttpGet(url.toURI());
        readHttpResponse(getHttpClient(url).execute(httpget), 200);
    }

    @Test
    public void testGetWithIncorrectQueryParameter() throws Exception {
        URL url = buildURL(true, true, 1);
        HttpGet httpget = new HttpGet(url.toURI());
        readHttpResponse(getHttpClient(url).execute(httpget), 400);
    }

    @Test
    public void testGetWithHttpHeader() throws Exception {
        URL url = buildURL(true, false, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("org.wildfly.useStreamAsResponse", null);
        readHttpResponse(getHttpClient(url).execute(httpget), 200);
    }

    @Test
    public void testGetWithSpecifiedHttpHeader() throws Exception {
        URL url = buildURL(true, false, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("org.wildfly.useStreamAsResponse", "0");
        readHttpResponse(getHttpClient(url).execute(httpget), 200);
    }

    @Test
    public void testGetWithIncorrectHttpHeader() throws Exception {
        URL url = buildURL(true, false, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("org.wildfly.useStreamAsResponse", "1");
        readHttpResponse(getHttpClient(url).execute(httpget), 400);
    }

    @Test
    public void testGetWithMatchedContentType() throws Exception {
        URL url = buildURL(true, true, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("Accept", "text/plain");
        HttpResponse response = getHttpClient(url).execute(httpget);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("text/plain"));
    }

    @Test
    public void testGetWithUnmatchedContentType() throws Exception {
        URL url = buildURL(true, true, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("Accept", "text/html");
        HttpResponse response = getHttpClient(url).execute(httpget);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("application/octet-stream"));
    }

    @Test
    public void testGetWithUnmatchedOctetStreamContentType() throws Exception {
        URL url = buildURL(true, true, null);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setHeader("Accept", "application/octet-stream");
        HttpResponse response = getHttpClient(url).execute(httpget);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("application/octet-stream"));
    }

    @Test
    public void testPostWithQueryParameter() throws Exception {
        URL url = buildURL(false, true, null);
        HttpPost httpPost = getHttpPost(url);
        HttpResponse response = getHttpClient(url).execute(httpPost);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("text/plain"));
    }

    @Test
    public void testPostWithSpecifiedQueryParameter() throws Exception {
        URL url = buildURL(false, true, 0);
        HttpPost httpPost = getHttpPost(url);
        readHttpResponse(getHttpClient(url).execute(httpPost), 200);
    }

    @Test
    public void testPostWithIncorrectQueryParameter() throws Exception {
        URL url = buildURL(false, true, 1);
        HttpPost httpPost = getHttpPost(url);
        readHttpResponse(getHttpClient(url).execute(httpPost), 400);
    }

    @Test
    public void testPostWithHttpHeader() throws Exception {
        URL url = buildURL(false, false, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("org.wildfly.useStreamAsResponse", null);
        readHttpResponse(getHttpClient(url).execute(httpPost), 200);
    }

    @Test
    public void testPostWithSpecifiedHttpHeader() throws Exception {
        URL url = buildURL(false, false, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("org.wildfly.useStreamAsResponse", "0");
        readHttpResponse(getHttpClient(url).execute(httpPost), 200);
    }

    @Test
    public void testPostWithIncorrectHttpHeader() throws Exception {
        URL url = buildURL(false, false, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("org.wildfly.useStreamAsResponse", "1");
        readHttpResponse(getHttpClient(url).execute(httpPost), 400);
    }

    @Test
    public void testPostWithMatchedContentType() throws Exception {
        URL url = buildURL(false, true, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("Accept", "text/plain");
        HttpResponse response = getHttpClient(url).execute(httpPost);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("text/plain"));
    }

    @Test
    public void testPostWithUnmatchedContentType() throws Exception {
        URL url = buildURL(false, true, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("Accept", "text/html");
        HttpResponse response = getHttpClient(url).execute(httpPost);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("application/octet-stream"));
    }

    @Test
    public void testPostWithUnmatchedOctetStreamContentType() throws Exception {
        URL url = buildURL(false, true, null);
        HttpPost httpPost = getHttpPost(url);
        httpPost.setHeader("Accept", "application/octet-stream");
        HttpResponse response = getHttpClient(url).execute(httpPost);
        readHttpResponse(response, 200);

        String contentType = response.getEntity().getContentType().getValue();
        Assert.assertTrue(contentType, contentType.contains("application/octet-stream"));
    }

    private HttpClient getHttpClient(URL url) {

        shutdownHttpClient();
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        // To save setup hassles, have the client use the same credentials as the slave HC
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(DomainLifecycleUtil.SLAVE_HOST_USERNAME, DomainLifecycleUtil.SLAVE_HOST_PASSWORD);
        defaultHttpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort(), "ManagementRealm"), creds);
        httpClient = defaultHttpClient;
        return httpClient;
    }

    private URL buildURL(boolean forGet, boolean useHeader, Integer streamIndex) throws MalformedURLException {
        String filePart;
        if (forGet) {
            filePart = MGMT_CTX + "/host/slave/server/main-three/subsystem/log-stream-test?operation=attribute&name=log-file";
            if (useHeader) {
                filePart += "&" + getQueryParameter(streamIndex);
            }
        } else if (useHeader) {
            filePart = MGMT_CTX + "?" + getQueryParameter(streamIndex);
        } else {
            filePart = MGMT_CTX;
        }
        return new URL("http", DomainTestSupport.masterAddress, MGMT_PORT, filePart);
    }

    private static String getQueryParameter(Integer streamIndex) {
        String result = QUERY_PARAM;
        if (streamIndex != null) {
            result += "=" + streamIndex;
        }
        return result;
    }

    private HttpPost getHttpPost(URL url) throws URISyntaxException, UnsupportedEncodingException {
        // For POST we are using the custom op instead read-attribute that we use for GET
        // but this is just a convenient way to exercise the op (GET can't call custom ops),
        // and isn't some limitation of POST
        PathAddress base = PathAddress.pathAddress(HOST, "slave").append(SERVER, "main-three");
        ModelNode cmd = createReadAttributeOp(base);
        String cmdStr = cmd.toJSONString(true);
        HttpPost post = new HttpPost(url.toURI());
        StringEntity entity = new StringEntity(cmdStr);
        entity.setContentType(APPLICATION_JSON);
        post.setEntity(entity);

        return post;
    }

    private void readHttpResponse(HttpResponse response, int expectedStatus) throws IOException {

        StatusLine statusLine = response.getStatusLine();
        assertEquals(expectedStatus, statusLine.getStatusCode());

        if (expectedStatus == 200) {
            HttpEntity entity = response.getEntity();

            readLogStream(entity.getContent(), true, false);
        }

    }

    private ModelNode createReadAttributeOp(PathAddress base) {
        PathAddress pa = base.append(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME);
        return Util.getReadAttributeOperation(pa, LogStreamExtension.LOG_FILE.getName());
    }

    private ModelNode createOperationOp(PathAddress base) {
        PathAddress pa = base.append(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME);
        return Util.createEmptyOperation(LogStreamExtension.STREAM_LOG_FILE, pa);
    }

    private void readLogFile(ModelNode opNode, ModelControllerClient client, boolean forServer) throws IOException {
        Operation op = OperationBuilder.create(opNode).build();
        OperationResponse response = null;
        try {
            response = client.executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            System.out.println(respNode.toString());
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            ModelNode result = respNode.get("result");
            Assert.assertEquals(respNode.toString(), ModelType.STRING, result.getType());
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            Assert.assertEquals(1, streams.size());
            processResponseStream(response, result.asString(), forServer, client == masterClient);

        } finally {
            StreamUtils.safeClose(response);
        }

    }

    private void processResponseStream(OperationResponse response, String streamUUID, boolean forServer, boolean forMaster) throws IOException {
        OperationResponse.StreamEntry se = response.getInputStream(streamUUID);

        readLogStream(se.getStream(), forServer, forMaster);
    }


    private void readLogStream(InputStream stream, boolean forServer, boolean forMaster) throws IOException {

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream));

        String expected = LogStreamExtension.getLogMessage(logMessageContent);
        boolean readRegisteredServer = false;
        boolean readRegisteredSlave = false;
        boolean readExpected = false;
        String read;
        while ((read = reader.readLine()) != null) {
            readRegisteredServer = readRegisteredServer || read.contains("Registering server");
            readRegisteredSlave = readRegisteredSlave || read.contains("Registered remote slave host");
            readExpected = readExpected || read.contains(expected);
        }

        if (forServer) {
            Assert.assertFalse(readRegisteredServer);
        } else if (forMaster) {
            Assert.assertTrue(readRegisteredSlave);
        } else {
            Assert.assertFalse(readRegisteredSlave);
            Assert.assertTrue(readRegisteredServer);
        }
        Assert.assertTrue(readExpected);

        reader.close();

    }
    private static ModelNode executeForResult(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        try {
            return DomainTestUtils.executeForResult(op, modelControllerClient);
        } catch (MgmtOperationException e) {
            System.out.println(" Op failed:");
            System.out.println(e.getOperation());
            System.out.println("with result");
            System.out.println(e.getResult());
            throw e;
        }
    }
}
