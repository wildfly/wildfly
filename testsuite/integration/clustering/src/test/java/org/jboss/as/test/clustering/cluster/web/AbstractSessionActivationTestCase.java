/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.event.SessionActivationServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public abstract class AbstractSessionActivationTestCase extends AbstractClusteringTestCase {

    private final boolean transactional;

    protected AbstractSessionActivationTestCase(boolean transactional) {
        super(THREE_NODES);
        this.transactional = transactional;
    }

    @Test
    public void test(
            @ArquillianResource(SessionActivationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SessionActivationServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource(SessionActivationServlet.class) @OperateOnDeployment(DEPLOYMENT_3) URL baseURL3) throws Exception {

        URI uri1 = SessionActivationServlet.createURI(baseURL1);
        URI uri2 = SessionActivationServlet.createURI(baseURL2);
        URI uri3 = SessionActivationServlet.createURI(baseURL3);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            // Verify all operations on primary owner
            this.execute(client, new HttpPut(uri1), false);

            this.execute(client, new HttpGet(uri1), false);

            this.execute(client, new HttpDelete(uri1), false);

            // Verify all operations on both backup owner and non-owner
            this.execute(client, new HttpPut(uri2), true);

            this.execute(client, new HttpGet(uri2), false);

            this.execute(client, new HttpDelete(uri2), false);

            this.execute(client, new HttpPut(uri3), true);

            this.execute(client, new HttpGet(uri3), false);

            this.execute(client, new HttpDelete(uri3), false);
        }
    }

    private void execute(HttpClient client, HttpUriRequest request, boolean failover) throws IOException {
        if (failover && !this.transactional) {
            try {
                Thread.sleep(GRACE_TIME_TO_REPLICATE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HttpResponse response = client.execute(request);
        try {
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }
}
