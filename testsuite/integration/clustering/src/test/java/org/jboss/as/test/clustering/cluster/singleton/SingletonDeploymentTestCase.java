/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.singleton.servlet.TraceServlet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class SingletonDeploymentTestCase extends ClusterAbstractTestCase {

    @SuppressWarnings("unused")
    private final String deploymentName;

    protected SingletonDeploymentTestCase(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Test
    public void test(
            @ArquillianResource(TraceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
                    throws IOException, URISyntaxException, InterruptedException {

        HttpClient client = HttpClients.createDefault();

        URI uri1 = TraceServlet.createURI(baseURL1);
        // baseURL2 will not have been resolved, since the deployment would not have started
        URI uri2 = TraceServlet.createURI(new URL(baseURL2.getProtocol(), baseURL2.getHost(), baseURL2.getPort(), baseURL1.getFile()));

        try {
            HttpResponse response = client.execute(new HttpTrace(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpTrace(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(DEPLOYMENT_1);

            TimeUnit.SECONDS.sleep(5);

            response = client.execute(new HttpTrace(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpTrace(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(DEPLOYMENT_1);

            TimeUnit.SECONDS.sleep(5);

            response = client.execute(new HttpTrace(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpTrace(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(DEPLOYMENT_2);

            TimeUnit.SECONDS.sleep(5);

            response = client.execute(new HttpTrace(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpTrace(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(DEPLOYMENT_2);

            TimeUnit.SECONDS.sleep(5);

            response = client.execute(new HttpTrace(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpTrace(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }
}
