/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.manual.web;

import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

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
import org.jboss.as.test.clustering.managed.web.Mutable;
import org.jboss.as.test.clustering.managed.web.SimpleServlet;
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
public class LocalSessionPersistenceTestCase {

    private static final String CONTAINER = "node-non-ha";
    private static final String DEPLOYMENT = "node-non-ha";

    private static final String MODULE_NAME = LocalSessionPersistenceTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".war";

    @ArquillianResource
    private WildFlyContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APPLICATION_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        return war;
    }

    @Before
    public void beforeTestMethod() {
        NodeUtil.start(this.controller, CONTAINER);
        NodeUtil.deploy(this.deployer, DEPLOYMENT);
    }

    @After
    public void afterTestMethod() {
        NodeUtil.undeploy(this.deployer, DEPLOYMENT);
        NodeUtil.stop(this.controller, CONTAINER);
    }

    @Test
    public void testSessionPersistence(@ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT) URL baseURL, @ArquillianResource @OperateOnDeployment(DEPLOYMENT) ManagementClient managementClient) throws Exception {

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
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
                Assert.assertEquals(sessionId, response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            }

            NodeUtil.stop(this.controller, CONTAINER);
            NodeUtil.start(this.controller, CONTAINER);
            if (Boolean.getBoolean("ts.bootable") || Boolean.getBoolean("ts.bootable.ee9")) {
                NodeUtil.deploy(this.deployer, DEPLOYMENT);
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session passivation was configured but session was lost after restart.",
                        3, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertTrue(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
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
