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

package org.jboss.as.test.clustering.cluster.web.context;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for WFLY-4067
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class InvalidateConversationTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = InvalidateConversationTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.addClasses(ConversationServlet.class, ConversationBean.class, LogoutServlet.class);
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void testInvalidate(
            @ArquillianResource(ConversationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(ConversationServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        String conversation = null;

        establishTopology(baseURL1, TWO_NODES);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(ConversationServlet.createURI(baseURL1)));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(ConversationServlet.COUNT_HEADER));
                assertEquals(1, Integer.parseInt(response.getFirstHeader(ConversationServlet.COUNT_HEADER).getValue()));
                conversation = response.getFirstHeader(ConversationServlet.CONVERSATION_ID).getValue();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(ConversationServlet.createURI(baseURL2, conversation)));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(ConversationServlet.COUNT_HEADER));
                assertEquals(2, Integer.parseInt(response.getFirstHeader(ConversationServlet.COUNT_HEADER).getValue()));
                assertEquals(conversation, response.getFirstHeader(ConversationServlet.CONVERSATION_ID).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(LogoutServlet.createURI(baseURL1, conversation)));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(ConversationServlet.COUNT_HEADER));
                assertEquals(3, Integer.parseInt(response.getFirstHeader(ConversationServlet.COUNT_HEADER).getValue()));
                assertEquals(conversation, response.getFirstHeader(ConversationServlet.CONVERSATION_ID).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    private static void establishTopology(URL baseURL, String... nodes) throws URISyntaxException, IOException {
        ClusterHttpClientUtil.establishTopology(baseURL, "web", DEPLOYMENT_NAME, nodes);
    }
}
