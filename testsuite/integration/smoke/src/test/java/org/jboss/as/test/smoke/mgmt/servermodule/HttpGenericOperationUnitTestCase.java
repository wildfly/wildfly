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

package org.jboss.as.test.smoke.mgmt.servermodule;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.smoke.mgmt.servermodule.archive.sar.Simple;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Emanuel Muckenhuber
 */
@RunAsClient
@RunWith(Arquillian.class)
public class HttpGenericOperationUnitTestCase {

    private static final int RANDOM_FILE_SIZE = 10 * 1024 * 1024;
    private static final String HTTP_PATH = "/management-upload";
    private static final String DMR_ENCODED = "application/dmr-encoded";
    private static final String MANAGEMENT_REALM = "ManagementRealm";

    @ContainerResource
    private ManagementClient managementClient;

    private URI uri;
    private HttpClient httpClient;
    private File randomContent;

    @Before
    public void setUp() throws Exception {
        final String host = managementClient.getMgmtAddress();
        final int port = managementClient.getMgmtPort();
        this.uri = new URI("http://" + host + ":" + port + HTTP_PATH);
        httpClient = createHttpClient(host, port, Authentication.USERNAME, Authentication.PASSWORD);
        randomContent = createRandomFile(RANDOM_FILE_SIZE);
    }

    @After
    public void shutdown() {
        httpClient.getConnectionManager().shutdown();
        if (randomContent != null) {
            randomContent.delete();
        }
    }

    @Test
    public void testCompositeDeploymentOperation() throws IOException {
        testDeploymentOperations(2, false);
    }

    @Test
    public void testDMREncodedOperation() throws Exception {
        testDeploymentOperations(1, true);
    }

    /**
     * Test the deployment operation. This will add and remove a given set of deployment using a composite operation
     * and attaching the streams necessary to the http post message.
     *
     * @param quantity the amount of deployments
     * @param encoded  whether to send the operation in the dmr encoded format or not
     * @throws IOException
     */
    private void testDeploymentOperations(final int quantity, final boolean encoded) throws IOException {

        // Create the deployment
        final File temp = createTempDeploymentZip();
        try {
            final ModelNode deployment = createCompositeDeploymentOperation(quantity);
            final ContentBody operation = getOperationBody(deployment, encoded);
            final List<ContentBody> streams = new ArrayList<ContentBody>();
            for (int i = 0; i < quantity; i++) {
                streams.add(new FileBody(temp));
            }

            final ModelNode response = executePost(operation, encoded, streams);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());

        } finally {
            temp.delete();
        }

        // And remove the deployments again
        final ModelNode remove = removeDeploymentsOperation(quantity);
        final ContentBody operation = getOperationBody(remove, encoded);
        final ModelNode response = executePost(operation, encoded);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());

    }

    private ModelNode executePost(final ContentBody operation, final boolean encoded) throws IOException {
        return executePost(operation, encoded, Collections.<ContentBody>emptyList());
    }

    /**
     * Execute the post request.
     *
     * @param operation the operation body
     * @param encoded   whether it should send the dmr encoded header
     * @param streams   the optional input streams
     * @return the response from the server
     * @throws IOException
     */
    private ModelNode executePost(final ContentBody operation, final boolean encoded, final List<ContentBody> streams) throws IOException {
        final HttpPost post = new HttpPost(uri);
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("operation", operation);
        for (ContentBody stream : streams) {
            entity.addPart("input-streams", stream);
        }
        post.setEntity(entity);

        return parseResponse(httpClient.execute(post), encoded);
    }

    private ContentBody getOperationBody(final ModelNode operation, final boolean encoded) throws IOException {
        if (encoded) {
            return new DMRContentEncodedBody(operation);
        } else {
            return new StringBody(operation.toJSONString(true));
        }
    }

    private final File createTempDeploymentZip() throws IOException {
        // Reuse the test deployment and add the random content file
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-http-deployment.sar")
                .add(new FileAsset(randomContent), "file")
                .addPackage(Simple.class.getPackage());

        File temp = null;
        try {
            temp = File.createTempFile("test", "http-deployment");
            archive.as(ZipExporter.class).exportTo(temp, true);
        } catch (IOException e) {
            if (temp != null) {
                temp.delete();
            }
            throw e;
        }
        return temp;
    }

    private ModelNode createCompositeDeploymentOperation(final int quantity) {

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).addEmptyList();

        final ModelNode steps = composite.get(STEPS).setEmptyList();

        for (int i = 0; i < quantity; i ++) {
            final ModelNode deploymentOne = steps.add();
            deploymentOne.get(OP).set(ADD);
            deploymentOne.get(OP_ADDR).set(DEPLOYMENT, "deployment-" + i);
            deploymentOne.get(ENABLED).set(true);
            deploymentOne.get(CONTENT).add().get(INPUT_STREAM_INDEX).set(i);
        }

        return composite;
    }

    private ModelNode removeDeploymentsOperation(final int quantity) {

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).addEmptyList();

        final ModelNode steps = composite.get(STEPS).setEmptyList();

        for (int i = 0; i < quantity; i ++) {
            final ModelNode deploymentOne = steps.add();
            deploymentOne.get(OP).set(REMOVE);
            deploymentOne.get(OP_ADDR).set(DEPLOYMENT, "deployment-" + i);
        }

        return composite;
    }

    private ModelNode parseResponse(HttpResponse response, boolean encoded) {
        try {
            String content = EntityUtils.toString(response.getEntity());
            int status = response.getStatusLine().getStatusCode();
            ModelNode modelResponse;
            if (status == HttpStatus.SC_OK) {
                if (encoded) {
                    modelResponse = ModelNode.fromBase64(new ByteArrayInputStream(content.getBytes()));
                    Assert.assertTrue(response.getFirstHeader("Content-Type").getValue().contains(DMR_ENCODED));
                } else {
                    modelResponse = ModelNode.fromJSONString(content);
                }
            } else {
                modelResponse = new ModelNode();
                modelResponse.get(OUTCOME).set(FAILED);
                modelResponse.get(FAILURE_DESCRIPTION).set(content);
            }
            return modelResponse;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read response content as String");
        }
    }

    private static HttpClient createHttpClient(String host, int port, String username, String password) {
        PoolingClientConnectionManager connectionPool = new PoolingClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(connectionPool);
        SchemeRegistry schemeRegistry = httpClient.getConnectionManager().getSchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        try {
            schemeRegistry.register(new Scheme("https", 443, new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }, new AllowAllHostnameVerifier())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        httpClient.setHttpRequestRetryHandler(new StandardHttpRequestRetryHandler(5, true));
        httpClient.getCredentialsProvider().setCredentials(
                new AuthScope(host, port, MANAGEMENT_REALM, AuthPolicy.DIGEST),
                new UsernamePasswordCredentials(username, password)
        );

        return httpClient;
    }

    private static final Random random = new Random();

    /**
     * Create a file with random content and a given size.
     *
     * @param size the file size
     * @return the created temp file
     * @throws IOException
     */
    private static File createRandomFile(int size) throws IOException {
        final byte[] buffer = new byte[16384];
        final File file = File.createTempFile("test", "artifact");
        try (final FileOutputStream os = new FileOutputStream(file)) {
            for (int length = 0; length < size; length += buffer.length) {
                random.nextBytes(buffer);
                os.write(buffer);
            }
        } catch (IOException e) {
            file.delete();
            throw e;
        }
        return file;
    }

    static class DMRContentEncodedBody extends AbstractContentBody {

        private final ModelNode model;

        DMRContentEncodedBody(ModelNode model) {
            super(DMR_ENCODED);
            this.model = model;
        }

        @Override
        public String getFilename() {
            return null;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            model.writeBase64(out);
        }

        @Override
        public String getCharset() {
            return null;
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_BINARY;
        }

        @Override
        public long getContentLength() {
            // Needed for the http client
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                model.writeBase64(os);
                return os.size();
            } catch (Exception e) {
                return -1;
            }
        }
    }

}
