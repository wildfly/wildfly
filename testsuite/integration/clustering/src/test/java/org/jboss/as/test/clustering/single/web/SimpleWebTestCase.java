/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.web;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validate the <distributable/> works for single node.
 *
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(SimpleWebTestCase.ServerSetupTask.class)
public class SimpleWebTestCase {

    private static final String MODULE_NAME = SimpleWebTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".war";

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APPLICATION_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        war.setWebXML(SimpleWebTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void test(@ArquillianResource(SimpleServlet.class) URL baseURL, @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient managementClient) throws IOException, URISyntaxException {
        // Validate existence of runtime resource for deployment cache
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "infinispan"), PathElement.pathElement("cache-container", "web"), PathElement.pathElement("cache", APPLICATION_NAME));
        ModelNode operation = Util.createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address);
        operation.get(ModelDescriptionConstants.NAME).set(new ModelNode("number-of-entries"));
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(0, result.get(ModelDescriptionConstants.RESULT).asInt());

        URI uri = SimpleServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
                // This won't be true unless we have somewhere to which to replicate or session persistence is configured (current default)
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }

        result = managementClient.getControllerClient().execute(operation);
        Assert.assertNotEquals(0, result.get(ModelDescriptionConstants.RESULT).asInt());
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super("single", createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            // Switch to primary-owner routing to validate WFLY-18095
                            .add("/subsystem=infinispan/cache-container=web/local-cache=routing:add")
                            .add("/subsystem=distributable-web/routing=infinispan:add(cache-container=web, cache=routing)")
                            .add("/subsystem=distributable-web/infinispan-session-management=default/affinity=primary-owner:add")
                            .endBatch()
                            .add("/subsystem=infinispan/cache-container=web/local-cache=passivation:write-attribute(name=statistics-enabled, value=true)")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .add("/subsystem=infinispan/cache-container=web/local-cache=passivation:undefine-attribute(name=statistics-enabled)")
                            .startBatch()
                            .add("/subsystem=distributable-web/infinispan-session-management=default/affinity=local:add")
                            .add("/subsystem=distributable-web/routing=infinispan:remove")
                            .add("/subsystem=infinispan/cache-container=web/local-cache=routing:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}
