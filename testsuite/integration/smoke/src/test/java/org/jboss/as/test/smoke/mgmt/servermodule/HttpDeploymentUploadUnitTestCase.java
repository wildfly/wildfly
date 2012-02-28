/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.mgmt.servermodule;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.smoke.mgmt.servermodule.archive.sar.Simple;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Test the HTTP API upload functionality to ensure that a deployment is successfully
 * transferred to the HTTP Server and processed by the model controller.
 *
 * @author Jonathan Pearlin
 */
@RunAsClient
@RunWith(Arquillian.class)
public class HttpDeploymentUploadUnitTestCase {

    private static final String BOUNDARY_PARAM = "NeAG1QNIHHOyB5joAS7Rox!!";

    private static final String BOUNDARY = "--" + BOUNDARY_PARAM;

    private static final String CRLF = "\r\n";

    private static final String POST_REQUEST_METHOD = "POST";

    public static final String MANAGEMENT_URL_PART = "management";

    private static final String UPLOAD_URL_PART = "add-content";

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testHttpDeploymentUpload() throws Exception {
        Authentication.setupDefaultAuthenticator();

        HttpURLConnection connection = null;
        BufferedOutputStream os = null;
        BufferedInputStream is = null;
        //TODO: hard coded port
        final String basicUrl = "http://" + managementClient.getMgmtAddress() + ":9990/" + MANAGEMENT_URL_PART;
        String uploadUrl = basicUrl + "/" + UPLOAD_URL_PART;

        try {
            // Create the HTTP connection to the upload URL
            connection =getHttpURLConnection(uploadUrl, "multipart/form-data; boundary=" + BOUNDARY_PARAM);

            // Grab the test WAR file and get a stream to its contents to be included in the POST.
            final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-http-deployment.sar", Simple.class.getPackage());
            os = new BufferedOutputStream(connection.getOutputStream());
            is = new BufferedInputStream(archive.as(ZipExporter.class).exportAsInputStream());

            // Write the POST request and read the response from the HTTP server.
            writeUploadRequest(is, os);
            // JBAS-9291
            assertEquals("text/html", connection.getHeaderField("Content-Type"));
            ModelNode node = readResult(connection.getInputStream());
            assertNotNull(node);
            System.out.println(node);
            assertEquals(SUCCESS, node.require(OUTCOME).asString());

            byte[] hash = node.require(RESULT).asBytes();

            connection.disconnect();

            connection = getHttpURLConnection(basicUrl, "application/json");
            os = new BufferedOutputStream(connection.getOutputStream());

            writeAddRequest(os, hash);

            node = readResult(connection.getInputStream());
            assertNotNull(node);
            System.out.println(node);
            assertEquals(SUCCESS, node.require(OUTCOME).asString());

            connection.disconnect();
        }
        finally {
            closeQuietly(is);
            closeQuietly(os);
            try {
                connection = getHttpURLConnection(basicUrl, "application/json");
                os = new BufferedOutputStream(connection.getOutputStream());
                writeRemoveRequest(os);
                ModelNode node = readResult(connection.getInputStream());
                System.out.println(node);
                connection.disconnect();
            } catch (Exception ignored) {
                ignored.printStackTrace();
            } finally {
                closeQuietly(os);
            }
        }
    }

    private HttpURLConnection getHttpURLConnection(final String url, final String contentType) throws IOException {
        HttpURLConnection connection =(HttpURLConnection) new URL(url).openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod(POST_REQUEST_METHOD);
        connection.setRequestProperty("Content-Type", contentType);
        return connection;
    }

    private void writeUploadRequest(final InputStream is, final OutputStream os) throws IOException {
        os.write(buildUploadPostRequestHeader());
        writePostRequestPayload(is, os);
        os.write((CRLF + BOUNDARY + "--" + CRLF).getBytes("US-ASCII"));
        os.flush();
    }

    private void writeAddRequest(BufferedOutputStream os, byte[] hash) throws IOException {

        ModelNode op = new ModelNode();
        op.get("operation").set("add");
        op.get("address").add("deployment", "test-http-deployment.sar");
        op.get("content").get(0).get("hash").set(hash);
        op.get("enabled").set(true);

        os.write(op.toJSONString(true).getBytes());
        os.flush();
    }

    private void writeRemoveRequest(BufferedOutputStream os) throws IOException {

        ModelNode op = new ModelNode();
        op.get("operation").set("remove");
        op.get("address").add("deployment", "test-http-deployment.sar");

        os.write(op.toJSONString(true).getBytes());
        os.flush();
    }

    private ModelNode readResult(final InputStream is) throws IOException, NoSuchElementException {
        return ModelNode.fromJSONStream(is);
    }

    private byte[] buildUploadPostRequestHeader() {
        final StringBuilder builder = new StringBuilder();
        builder.append("blah blah blah preamble blah blah blah");
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test1\"", "", "test1"));
        builder.append(CRLF);
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test2\"", "", "test2"));
        builder.append(CRLF);
        builder.append(buildPostRequestHeaderSection("form-data; name=\"file\"; filename=\"test.war\"", "application/octet-stream", ""));

        return builder.toString().getBytes();
    }

    private String buildPostRequestHeaderSection(final String contentDisposition, final String contentType, final String content) {
        final StringBuilder builder = new StringBuilder();
        builder.append(BOUNDARY);
        builder.append(CRLF);
        if(contentDisposition != null && contentDisposition.length() > 0) {
            builder.append(String.format("Content-Disposition: %s", contentDisposition));
            builder.append(CRLF);
        }

        if(contentType != null && contentType.length() > 0) {
            builder.append(String.format("Content-Type: %s", contentType));
            builder.append(CRLF);
        }

        builder.append(CRLF);

        if(content != null && content.length() > 0) {
            builder.append(content);
        }
        return builder.toString();
    }

    private void writePostRequestPayload(final InputStream is, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[1024];
        int numRead = 0;

        while(numRead > -1) {
            numRead = is.read(buffer);
            if(numRead > 0) {
                os.write(buffer,0,numRead);
            }
        }
    }

    private void closeQuietly(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {}
        }
    }
}
