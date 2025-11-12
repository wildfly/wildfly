/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.web;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.CONTAINER_SINGLE;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DEPLOYMENT_1;
import static org.jboss.as.test.shared.util.AssumeTestGroupUtil.isBootableJar;

import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that session passivation in non-HA environment works (on single node).
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class NonHaWebSessionPersistenceTestCase {
    private static final String MODULE_NAME = NonHaWebSessionPersistenceTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".war";

    @ArquillianResource
    private WildFlyContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APPLICATION_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        war.setWebXML(NonHaWebSessionPersistenceTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Before
    public void beforeTestMethod() {
        NodeUtil.start(this.controller, CONTAINER_SINGLE);
        NodeUtil.deploy(this.deployer, DEPLOYMENT_1);
    }

    @After
    public void afterTestMethod() {
        NodeUtil.undeploy(this.deployer, DEPLOYMENT_1);
        NodeUtil.stop(this.controller, CONTAINER_SINGLE);
    }

    @Test
    public void testSessionPersistence(@ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL, @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient managementClient) throws Exception {

        URI url = SimpleServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            String sessionId = null;
            try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                sessionId = response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue();
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertFalse(Boolean.parseBoolean(response.getFirstHeader("serialized").getValue()));
                Assert.assertEquals(sessionId, response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            }

            NodeUtil.stop(this.controller, CONTAINER_SINGLE, AbstractClusteringTestCase.SUSPEND_TIMEOUT_S);
            NodeUtil.start(this.controller, CONTAINER_SINGLE);
            if (isBootableJar()) {
                NodeUtil.deploy(this.deployer, DEPLOYMENT_1);
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session passivation was configured but session was lost after restart.",
                        3, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertTrue(Boolean.parseBoolean(response.getFirstHeader("serialized").getValue()));
                Assert.assertEquals(sessionId, response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            }

            String invalidationRequest = String.format("/deployment=%s/subsystem=undertow:invalidate-session(session-id=%s)", APPLICATION_NAME, sessionId);
            ClusterTestUtil.execute(managementClient, invalidationRequest);

            try (CloseableHttpResponse response = client.execute(new HttpHead(url))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SimpleServlet.SESSION_ID_HEADER));
            }
        }
    }
}
