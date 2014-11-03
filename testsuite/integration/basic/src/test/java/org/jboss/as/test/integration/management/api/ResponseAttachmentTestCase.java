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

package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
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
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.extension.EmptySubsystemParser;
import org.jboss.as.test.integration.management.extension.ExtensionUtils;
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
import org.junit.runner.RunWith;

/**
 * Test of streams attached to a standalone server management op response.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResponseAttachmentTestCase extends ContainerResourceMgmtTestBase {

    private static final Logger log = Logger.getLogger(ResponseAttachmentTestCase.class);

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    private static final String QUERY_PARAM = "useStreamAsResponse";
    private static final String APPLICATION_JSON = "application/json";

    private String logMessageContent;
    private HttpClient httpClient;

    @BeforeClass
    public static void beforeClass() throws IOException {
        ExtensionUtils.createExtensionModule(LogStreamExtension.MODULE_NAME, LogStreamExtension.class,
                EmptySubsystemParser.class.getPackage());
    }

    @AfterClass
    public static void afterClass() {
        ExtensionUtils.deleteExtensionModule(LogStreamExtension.MODULE_NAME);
    }

    @Before
    public void before() throws IOException, MgmtOperationException {
        // Install the log-stream extension and subsystem

        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
        executeOperation(op);

        op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
        executeOperation(op);

        logMessageContent = String.valueOf(System.currentTimeMillis());
        op = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        op.get(VALUE).set(logMessageContent);
        executeOperation(op);
    }

    @After
    public void after() throws IOException, MgmtOperationException {

        shutdownHttpClient();

        try {
            ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
            executeOperation(op);
        } finally {
            try {
                ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
                executeOperation(op);
            } finally {
                ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
                executeOperation(op);
            }
        }
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
    public void testNativeInterface() throws Exception {
        ModelNode opNode = Util.getReadAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME), "log-file");
        readLogFile(opNode);

        opNode = Util.createEmptyOperation(LogStreamExtension.STREAM_LOG_FILE, PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
        readLogFile(opNode);
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

    private URL buildURL(boolean forGet, boolean useHeader, Integer streamIndex) throws MalformedURLException {
        String filePart;
        if (forGet) {
            filePart = MGMT_CTX + "/subsystem/log-stream-test?operation=attribute&name=log-file";
            if (useHeader) {
                filePart += "&" + getQueryParameter(streamIndex);
            }
        } else if (useHeader) {
            filePart = MGMT_CTX + "?" + getQueryParameter(streamIndex);
        } else {
            filePart = MGMT_CTX;
        }
        return new URL("http", getManagementClient().getMgmtAddress(), MGMT_PORT, filePart);
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
        ModelNode cmd = Util.createEmptyOperation(LogStreamExtension.STREAM_LOG_FILE, PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
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

            readLogStream(entity.getContent());
        }

    }

    private void readLogFile(ModelNode opNode) throws IOException {

        Operation op = OperationBuilder.create(opNode).build();
        OperationResponse response = null;
        try {
            response = getModelControllerClient().executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            Assert.assertEquals(respNode.toString(), ModelType.STRING, respNode.get("result").getType());
            String uuid = respNode.get("result").asString();
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            Assert.assertEquals(1, streams.size());
            OperationResponse.StreamEntry se = streams.get(0);
            Assert.assertEquals(uuid, se.getUUID());
            readLogStream(se.getStream());

        } finally {
            StreamUtils.safeClose(response);
        }

    }

    private void readLogStream(InputStream stream) throws IOException {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream));
        String read;
        boolean readMessage = false;
        String expected = LogStreamExtension.getLogMessage(logMessageContent);
        while ((read = reader.readLine()) != null) {
            readMessage = readMessage || read.contains(expected);
            //System.out.println(read);
        }

        Assert.assertTrue("Did not see " + expected, readMessage);

    }

    private HttpClient getHttpClient(URL url) {

        shutdownHttpClient();

        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(Authentication.USERNAME, Authentication.PASSWORD);
        defaultHttpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort(), "ManagementRealm"), creds);
        httpClient = defaultHttpClient;

        return httpClient;
    }
}
