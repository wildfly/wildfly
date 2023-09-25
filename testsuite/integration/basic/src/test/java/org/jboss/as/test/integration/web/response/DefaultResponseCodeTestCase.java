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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
public class DefaultResponseCodeTestCase extends ContainerResourceMgmtTestBase {
    private static final String URL_PATTERN = "simple";
    @ArquillianResource
    URL url;
    private HttpClient httpclient = null;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DefaultResponseCodeTestCase.class.getSimpleName() + ".war");
        war.addClasses(SimpleServlet.class);
        return war;
    }

    @Before
    public void setup() {
        this.httpclient = HttpClientBuilder.create().build();
    }

    @Test
    public void testNormalOpMode() throws Exception {
        HttpGet httpget = new HttpGet(url.toString() + URL_PATTERN);
        HttpResponse response = this.httpclient.execute(httpget);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        httpget = new HttpGet(url.toString() + URL_PATTERN+"/xxx");
        response = this.httpclient.execute(httpget);
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testDefaultResponseCode() throws Exception {

        try (AutoCloseable snapshot = ServerSnapshot.takeSnapshot(getManagementClient())){
            ModelNode operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "write-attribute");
            operation.get("name").set("default-response-code");
            operation.get("value").set(506);
            executeOperation(operation);
            operation = createOpNode("subsystem=undertow/server=default-server/host=default-host", "remove");
            operation.get("address").add("location","/");
            executeOperation(operation);
            executeReloadAndWaitForCompletion(getManagementClient());
            HttpGet httpget = new HttpGet(url.toString() + URL_PATTERN);
            HttpResponse response = this.httpclient.execute(httpget);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            String badUrl = url.toString();
            badUrl = badUrl.substring(0,badUrl.length()-1);
            httpget = new HttpGet(badUrl + "xxx/xxx");
            response = this.httpclient.execute(httpget);
            Assert.assertEquals(506, response.getStatusLine().getStatusCode());
        }
    }

}
