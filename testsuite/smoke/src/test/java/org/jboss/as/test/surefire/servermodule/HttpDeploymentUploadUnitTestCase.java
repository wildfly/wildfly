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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.EmbeddedServerFactory;
import org.jboss.as.server.Main;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the HTTP API upload functionality to ensure that a deployment is successfully
 * transferred to the HTTP Server and processed by the model controller.
 *
 * @author Jonathan Pearlin
 */
public class HttpDeploymentUploadUnitTestCase {

    private static final String BOUNDARY = "-----------------------------261773107125236";

    private static final String CRLF = "\r\n";

    private static final String POST_REQUEST_METHOD = "POST";

    private static final String TEST_WAR = "demos/war-example.war";

    private static final String UPLOAD_URL = "http://localhost:9990/domain-api/add-content";

    private static ServiceContainer container;

    @BeforeClass
    public static void startServer() throws Exception {
        EmbeddedServerFactory.setupCleanDirectories(System.getProperties());
        final ServerEnvironment serverEnvironment = Main.determineEnvironment(new String[0], new Properties(System.getProperties()), System.getenv());
        assertNotNull(serverEnvironment);
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        final Bootstrap.Configuration configuration = new Bootstrap.Configuration();
        configuration.setServerEnvironment(serverEnvironment);
        configuration.setModuleLoader(Module.getBootModuleLoader());
        configuration.setPortOffset(0);

        container = bootstrap.startup(configuration, Collections.<ServiceActivator>emptyList()).get();
        assertNotNull(container);
    }

    @AfterClass
    public static void testServerStartupAndShutDown() throws Exception {
        container.shutdown();
        container.awaitTermination();
        assertTrue(container.isShutdownComplete());
    }

    @Test
    public void testHttpDeploymentUpload() {
        BufferedOutputStream os = null;
        BufferedInputStream is = null;

        try {
            // Create the HTTP connection to the upload URL
            final HttpURLConnection connection =(HttpURLConnection) new URL(UPLOAD_URL).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST_METHOD);

            // Grab the test WAR file and get a stream to its contents to be included in the POST.
            final WebArchive archive = ShrinkWrapUtils.createWebArchive(TEST_WAR);
            os = new BufferedOutputStream(connection.getOutputStream());
            is = new BufferedInputStream(archive.as(ZipExporter.class).exportZip());

            // Write the POST request and read the response from the HTTP server.
            writeRequest(is, os);
            final ModelNode node = readResult(connection.getInputStream());
            assertNotNull(node);
            assertEquals(SUCCESS, node.require(OUTCOME).asString());
        } catch (final Exception e) {
            fail("Exception not expected: " + e.getMessage());
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    private void writeRequest(final InputStream is, final OutputStream os) throws IOException {
        os.write(buildPostRequestHeader());
        writePostRequestPayload(is, os);
        os.write(buildPostRequestFooter());
        os.flush();
    }

    private ModelNode readResult(final InputStream is) throws IOException, NoSuchElementException {
        return ModelNode.fromJSONStream(is);
    }

    private byte[] buildPostRequestHeader() {
        final StringBuilder builder = new StringBuilder();
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test1\"", "", "test1"));
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test2\"", "", "test2"));
        builder.append(buildPostRequestHeaderSection("form-data; name=\"file\"; filename=\"test.war\"", "application/octet-stream", ""));
        return builder.toString().getBytes();
    }

    private String buildPostRequestHeaderSection(final String contentDisposition, final String contentType, final String content) {
        final StringBuilder builder = new StringBuilder();
        builder.append(BOUNDARY);
        builder.append(CRLF);
        if(contentDisposition != null && contentDisposition.length() > 0) {
            builder.append(String.format("Content-Disposition: %s", contentDisposition));
        }
        builder.append(CRLF);
        if(contentType != null && contentType.length() > 0) {
            builder.append(String.format("Content-Type: %s", contentType));
        }
        builder.append(CRLF);
        if(content != null && content.length() > 0) {
            builder.append(content);
        }
        builder.append(CRLF);
        return builder.toString();
    }

    private byte[] buildPostRequestFooter() {
        final StringBuilder builder = new StringBuilder();
        builder.append(CRLF);
        builder.append(BOUNDARY);
        builder.append("--");
        builder.append(CRLF);
        return builder.toString().getBytes();
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
