/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.stateful;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.TimeoutIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.servlet.StatefulServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates failover of a SFSB in different contexts
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class StatefulTimeoutTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = StatefulTimeoutTestCase.class.getSimpleName();
    private static final long WAIT_FOR_TIMEOUT = TimeoutUtil.adjust(5000);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addClasses(TimeoutIncrementorBean.class, Incrementor.class);
        war.addPackage(StatefulServlet.class.getPackage());
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulServlet.class.getPackage(), "web.xml");
        return war;
    }

    /**
     * Validates that a @Stateful(passivationCapable=false) bean does not replicate
     */
    @Test
    public void timeout(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = StatefulServlet.createURI(baseURL1, MODULE_NAME, TimeoutIncrementorBean.class.getSimpleName());
        URI uri2 = StatefulServlet.createURI(baseURL2, MODULE_NAME, TimeoutIncrementorBean.class.getSimpleName());

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertEquals(1, queryCount(client, uri1));
            assertEquals(2, queryCount(client, uri1));

            // Make sure state replicated correctly
            assertEquals(3, queryCount(client, uri2));
            assertEquals(4, queryCount(client, uri2));

            Thread.sleep(WAIT_FOR_TIMEOUT);

            // SFSB should have timed out
            assertEquals(0, queryCount(client, uri1));
            // Subsequent request will create it again
            assertEquals(1, queryCount(client, uri1));

            Thread.sleep(WAIT_FOR_TIMEOUT);

            // Make sure SFSB times out on other node too
            assertEquals(0, queryCount(client, uri2));
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
