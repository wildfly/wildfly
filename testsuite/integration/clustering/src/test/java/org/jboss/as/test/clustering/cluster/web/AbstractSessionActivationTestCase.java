/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

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
        super(NODE_1_2_3);
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

            this.execute(client, new HttpDelete(uri2), true);

            this.execute(client, new HttpPut(uri3), true);

            this.execute(client, new HttpGet(uri3), false);

            this.execute(client, new HttpDelete(uri3), true);
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
