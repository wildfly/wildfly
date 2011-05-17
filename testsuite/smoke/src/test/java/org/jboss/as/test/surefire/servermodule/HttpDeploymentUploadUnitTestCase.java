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
package org.jboss.as.test.surefire.servermodule;

import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.as.test.surefire.servermodule.archive.sar.Simple;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;

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
@Ignore("Test migrated to managed container")
public class HttpDeploymentUploadUnitTestCase extends AbstractServerInModuleTestCase {

    private static final String BOUNDARY_PARAM = "NeAG1QNIHHOyB5joAS7Rox!!";

    private static final String BOUNDARY = "--" + BOUNDARY_PARAM;

    private static final String CRLF = "\r\n";

    private static final String POST_REQUEST_METHOD = "POST";

    private static final String BASIC_URL = "http://localhost:9990/domain-api/";

    private static final String UPLOAD_URL = BASIC_URL + "add-content";

    @Test
    public void testHttpDeploymentUpload() throws Exception {
        BufferedOutputStream os = null;
        BufferedInputStream is = null;

        try {
            // Create the HTTP connection to the upload URL
            HttpURLConnection connection =(HttpURLConnection) new URL(UPLOAD_URL).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST_METHOD);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_PARAM);


            // Grab the test WAR file and get a stream to its contents to be included in the POST.
//            final WebArchive archive = ShrinkWrapUtils.createWebArchive(TEST_WAR, SimpleServlet.class.getPackage());
            final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar", Simple.class.getPackage());
            os = new BufferedOutputStream(connection.getOutputStream());
            is = new BufferedInputStream(archive.as(ZipExporter.class).exportZip());

            // Write the POST request and read the response from the HTTP server.
            writeUploadRequest(is, os);
            // JBAS-9291
            assertEquals("text/html", connection.getHeaderField("Content-Type"));
            ModelNode node = readResult(connection.getInputStream());
            assertNotNull(node);
            System.out.println(node);
            assertEquals(SUCCESS, node.require(OUTCOME).asString());

            byte[] hash = node.require(RESULT).asBytes();

            connection =(HttpURLConnection) new URL(BASIC_URL).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST_METHOD);
            os = new BufferedOutputStream(connection.getOutputStream());

            writeAddRequest(os, hash);

            node = readResult(connection.getInputStream());
            assertNotNull(node);
            System.out.println(node);
            assertEquals(SUCCESS, node.require(OUTCOME).asString());

//        } catch (final Exception e) {
//            fail("Exception not expected: " + e.getMessage());
        }
        finally {
            closeQuietly(is);
            closeQuietly(os);
        }
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
        op.get("address").add("deployment", "test-deployment.sar");
        op.get("content").get(0).get("hash").set(hash);
        op.get("enabled").set(true);

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
