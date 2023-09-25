/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.response;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test to check if server will return default code if no app is registered under path.
 *
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultResponseCodeAtRootTestCase extends ContainerResourceMgmtTestBase {
    private static final String URL_PATTERN = "/";
    private URL url;
    @ArquillianResource
    Deployer deployer;

    private HttpClient httpclient = null;

    @Deployment(testable = false, managed = false, name = "test")
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DefaultResponseCodeAtRootTestCase.class.getSimpleName() + ".war");
        war.addClasses(SimpleServlet.class);
        war.addAsWebInfResource(new StringAsset(
                "<jboss-web>"+
                        "<context-root>/</context-root>"+
                "</jboss-web>"),"jboss-web.xml");
        return war;
    }

    @Before
    public void setup() throws Exception {
        this.httpclient = HttpClientBuilder.create().build();
        this.url = super.getManagementClient().getWebUri().toURL();
    }

    @Test
    public void testNormalOpMode() throws Exception {
        deployer.deploy("test");
        try {
            HttpGet httpget = new HttpGet(url.toString());
            HttpResponse response = this.httpclient.execute(httpget);
            //403 apparently
            Assert.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
            httpget = new HttpGet(url.toString() + URL_PATTERN + "xxx");
            response = this.httpclient.execute(httpget);
            Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        } finally {
            deployer.undeploy("test");
        }
    }

    @Test
    public void testDefaultResponseCode() throws Exception {

        try (AutoCloseable snapshot = ServerSnapshot.takeSnapshot(getManagementClient())){
            CompositeOperationBuilder cob = CompositeOperationBuilder.create();

            ModelNode operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "write-attribute");
            operation.get("name").set("default-response-code");
            operation.get("value").set(506);
            cob.addStep(operation);
            // if location service is removed, if no deployment == no virtual host.
            operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "remove");
            operation.get("address").add("location","/");
            cob.addStep(operation);
            executeOperation(cob.build().getOperation());
            executeReloadAndWaitForCompletion(getManagementClient());
            deployer.deploy("test");
            HttpGet httpget = null;
            HttpResponse response = null;
            httpget = new HttpGet(url.toString() + URL_PATTERN+"xxx/xxxxx");
            response = this.httpclient.execute(httpget);
            Assert.assertEquals(404, response.getStatusLine().getStatusCode());
            deployer.undeploy("test");
            httpget = new HttpGet(url.toString() + URL_PATTERN);
            response = this.httpclient.execute(httpget);
            Assert.assertEquals(""+httpget,506, response.getStatusLine().getStatusCode());
        }
    }
}
