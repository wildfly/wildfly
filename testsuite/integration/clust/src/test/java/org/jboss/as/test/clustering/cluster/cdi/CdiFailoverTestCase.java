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
package org.jboss.as.test.clustering.cluster.cdi;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Test failover with CDI session bean.
 * @author Tomas Remes
 */

@RunWith(Arquillian.class)
@RunAsClient
public class CdiFailoverTestCase extends ClusterAbstractTestCase {

    private static final String MODULE_NAME = "cdi-failover";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addClasses(Incrementor.class, CdiIncrementorBean.class, CdiServlet.class );
        war.setWebXML(CdiFailoverTestCase.class.getPackage(), "web.xml");
        log.info(war.toString(true));
        return war;
    }

    /**
     * Test simple graceful shutdown failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Shutdown first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Bring up the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws java.io.IOException
     * @throws InterruptedException
     * @throws java.net.URISyntaxException
     */
    @Test
    public void testGracefulSimpleFailover(
            @ArquillianResource(CdiServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(CdiServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {
        testFailoverOnShutdown(baseURL1, baseURL2);
    }

    /**
     * Test simple undeploy failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Undeploy application from the first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Redeploy application to the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testGracefulUndeployFailover(
            @ArquillianResource(CdiServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(CdiServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {
        testFailoverOnUndeploy(baseURL1, baseURL2);
    }

    private void testFailoverOnShutdown(URL baseURL1, URL baseURL2) throws IOException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        URI uri1 = CdiServlet.createURI(baseURL1);
        URI uri2 = CdiServlet.createURI(baseURL2);

        try {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            stop(CONTAINER_2);

            assertEquals(5, queryCount(client, uri1));
            assertEquals(6, queryCount(client, uri1));

            start(CONTAINER_2);

            assertEquals(7, queryCount(client, uri1));
            assertEquals(8, queryCount(client, uri1));

            assertEquals(9, queryCount(client, uri2));
            assertEquals(10, queryCount(client, uri2));

            stop(CONTAINER_1);

            assertEquals(11, queryCount(client, uri2));
            assertEquals(12, queryCount(client, uri2));

            start(CONTAINER_1);

            assertEquals(13, queryCount(client, uri1));
            assertEquals(14, queryCount(client, uri1));

            assertEquals(15, queryCount(client, uri2));
            assertEquals(16, queryCount(client, uri2));
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    private void testFailoverOnUndeploy(URL baseURL1, URL baseURL2) throws IOException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        URI uri1 = CdiServlet.createURI(baseURL1);
        URI uri2 = CdiServlet.createURI(baseURL2);

        try {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            undeploy(DEPLOYMENT_2);

            assertEquals(5, queryCount(client, uri1));
            assertEquals(6, queryCount(client, uri1));

            deploy(DEPLOYMENT_2);

            assertEquals(7, queryCount(client, uri1));
            assertEquals(8, queryCount(client, uri1));

            assertEquals(9, queryCount(client, uri2));
            assertEquals(10, queryCount(client, uri2));

            undeploy(DEPLOYMENT_1);

            assertEquals(11, queryCount(client, uri2));
            assertEquals(12, queryCount(client, uri2));

            deploy(DEPLOYMENT_1);

            assertEquals(13, queryCount(client, uri1));
            assertEquals(14, queryCount(client, uri1));

            assertEquals(15, queryCount(client, uri2));
            assertEquals(16, queryCount(client, uri2));
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    private static int queryCount(HttpClient client, URI uri) throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

}
