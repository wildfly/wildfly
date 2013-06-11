package org.jboss.as.test.clustering.cluster.web.passivation;

import java.io.IOException;
import java.net.URISyntaxException;
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebSimpleTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class SessionPassivationTestCase extends ClusterAbstractTestCase {

    static WebArchive getBaseDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "passivation.war");
        war.addClasses(SessionOperationServlet.class);
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebSimpleTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Override
    protected void setUp() {
    }

    @Before
    public void deployBeforeTestForInjection() {
        start(CONTAINERS);
        deploy(CONTAINER_1, DEPLOYMENT_1);
        deploy(CONTAINER_2, DEPLOYMENT_2);
    }

    @After
    public void testCleanup() {
        cleanDeployments();
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                     @ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
                             throws IOException, URISyntaxException, InterruptedException {
        DefaultHttpClient client1 = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();
        DefaultHttpClient client2 = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();
        String session1 = null;
        String session2 = null;
        try {
            // This should not trigger any passivation/activation events
            HttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "1")));
            try {
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ACTIVATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.PASSIVATED_SESSIONS));
                session1 = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This will trigger passivation of session1
            response = client2.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "2")));
            try {
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ACTIVATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.PASSIVATED_SESSIONS));
                session2 = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Thread.sleep(500);

            // Verify session1 was passivated
            response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL1, "a")));
            try {
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ACTIVATED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.PASSIVATED_SESSIONS));
                Assert.assertEquals(session1, response.getFirstHeader(SessionOperationServlet.PASSIVATED_SESSIONS).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This should trigger activation event and passivation of session2
            response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL1, "a")));
            try {
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.ACTIVATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.PASSIVATED_SESSIONS));
                Assert.assertEquals(session1, response.getFirstHeader(SessionOperationServlet.ACTIVATED_SESSIONS).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Thread.sleep(500);

            // Verify session2 was passivated
            response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL1, "a")));
            try {
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ACTIVATED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.PASSIVATED_SESSIONS));
                Assert.assertEquals(session2, response.getFirstHeader(SessionOperationServlet.PASSIVATED_SESSIONS).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client1);
            HttpClientUtils.closeQuietly(client2);
        }
    }
}
