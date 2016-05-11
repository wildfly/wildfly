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

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.singleton.servlet.TraceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class SingletonDeploymentTestCase extends ClusterAbstractTestCase {

    private static final String DEPLOYMENT_NAME = "singleton-deployment-helper.war";
    static final String SINGLETON_DEPLOYMENT_1 = "singleton-deployment-0";
    static final String SINGLETON_DEPLOYMENT_2 = "singleton-deployment-1";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deploymentHelper0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deploymentHelper1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addPackage(TraceServlet.class.getPackage());
        return war;
    }

    public static final int DELAY = TimeoutUtil.adjust(5000);

    private final String deploymentName;

    SingletonDeploymentTestCase(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Test
    public void test(
            @ArquillianResource(TraceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(TraceServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
                    throws IOException, URISyntaxException, InterruptedException {

        this.deploy(SINGLETON_DEPLOYMENT_1);
        Thread.sleep(DELAY);
        this.deploy(SINGLETON_DEPLOYMENT_2);
        Thread.sleep(DELAY);

        URI uri1 = TraceServlet.createURI(new URL(baseURL1.getProtocol(), baseURL1.getHost(), baseURL1.getPort(), "/" + this.deploymentName + "/"));
        URI uri2 = TraceServlet.createURI(new URL(baseURL2.getProtocol(), baseURL2.getHost(), baseURL2.getPort(), "/" + this.deploymentName + "/"));

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(SINGLETON_DEPLOYMENT_1);

            Thread.sleep(DELAY);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(SINGLETON_DEPLOYMENT_1);

            Thread.sleep(DELAY);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(SINGLETON_DEPLOYMENT_2);

            Thread.sleep(DELAY);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(SINGLETON_DEPLOYMENT_2);

            Thread.sleep(DELAY);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            this.undeploy(SINGLETON_DEPLOYMENT_1, SINGLETON_DEPLOYMENT_2);
        }
    }
}
