/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.metrics.rbac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.MetricsHelper;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SnapshotRestoreSetupTask.class})
public class MicroProfileMetricsRbacTestCase {
    @ContainerResource
    private ManagementClient mgmtClient;

    @ArquillianResource
    private ContainerController containerController;

    @ArquillianResource
    URL url;

    private URL metricsUrl;
    private CloseableHttpClient client;
    private String vendorMetricsPrefix;

    @Before
    public void setup() throws Exception {
        client = HttpClients.createDefault();
        metricsUrl = new URL("http://" + mgmtClient.getMgmtAddress() + ":" + mgmtClient.getMgmtPort() + "/metrics");

        setRbacStatus("rbac");
        setEndpointSecurityStatus(true);

        ServerReload.reloadIfRequired(mgmtClient);

        vendorMetricsPrefix = MetricsHelper.getExpectedVendorMetricPrefix(mgmtClient);
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class,
                MicroProfileMetricsRbacTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class)
                ;
        return war;
    }

    @Test
    @InSequence(1)
    public void testAuthenticatedMetricsRequestWithInvalidRole() throws Exception {
        final CloseableHttpResponse response = makeRequest(metricsUrl, getHttpClientContext());
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String entity = EntityUtils.toString(response.getEntity());
        assertFalse(entity.contains(vendorMetricsPrefix));
    }

    @Test
    @InSequence(2)
    public void testAuthenticatedMetricsRequestWithValidRole() throws Exception {
        mapUserRole();

        final CloseableHttpResponse response = makeRequest(metricsUrl, getHttpClientContext());
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String entity = EntityUtils.toString(response.getEntity());
        assertTrue(entity.contains(vendorMetricsPrefix));
    }

    private HttpClientContext getHttpClientContext() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("testSuite", "testSuitePassword"));
        HttpClientContext hcContext = HttpClientContext.create();
        hcContext.setCredentialsProvider(credentialsProvider);
        return hcContext;
    }

    private void setRbacStatus(String provider) throws IOException {
        executeOperation(Util.getWriteAttributeOperation(
                PathAddress.parseCLIStyleAddress("/core-service=management/access=authorization"),
                "provider", provider)
        );
    }

    private void setEndpointSecurityStatus(boolean status) throws IOException {
        executeOperation(Util.getWriteAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=microprofile-metrics-smallrye"),
                "security-enabled", status)
        );
    }

    private void mapUserRole() throws IOException {
        final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();
        builder.addStep(Util.createAddOperation(
                PathAddress.parseCLIStyleAddress("/core-service=management/access=authorization/role-mapping=Monitor")));
        final ModelNode addOperation = Util.createAddOperation(
                PathAddress.parseCLIStyleAddress("/core-service=management/access=authorization/role-mapping=Monitor/include=monitor")
        );
        addOperation.get("alias").set("testSuite");
        addOperation.get("name").set("testSuite");
        addOperation.get("type").set("USER");
        builder.addStep(addOperation);
        executeOperation(builder.build());
    }

    private void executeOperation(final ModelNode op) throws IOException {
        executeOperation(Operation.Factory.create(op));
    }

    private void executeOperation(Operation op) throws IOException {
        final ModelNode response = mgmtClient.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(response)) {
            Assert.fail(Operations.getFailureDescription(response).asString());
        }
    }

    private CloseableHttpResponse makeRequest(URL endpointUrl, HttpContext context) throws IOException {
        return client.execute(new HttpGet(endpointUrl.toExternalForm()), context);
    }
}
