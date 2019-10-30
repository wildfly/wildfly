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

package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.jboss.as.test.clustering.cluster.web.externalizer.CounterExternalizer;
import org.jboss.as.test.clustering.cluster.web.externalizer.CounterServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class ExternalizerTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = ExternalizerTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addPackage(CounterServlet.class.getPackage());
        war.setWebXML(ExternalizerTestCase.class.getPackage(), "web.xml");
        war.addAsServiceProvider(Externalizer.class, CounterExternalizer.class);
        return war;
    }

    @Test
    public void test(
            @ArquillianResource(CounterServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(CounterServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        URI uri1 = CounterServlet.createURI(baseURL1);
        URI uri2 = CounterServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertValue(client, uri1, 1);
            assertValue(client, uri1, 2);

            assertValue(client, uri2, 3);
            assertValue(client, uri2, 4);

            assertValue(client, uri1, 5);
            assertValue(client, uri1, 6);
        }
    }

    private static void assertValue(HttpClient client, URI uri, int value) throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri));
        try {
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(value, Integer.parseInt(response.getFirstHeader(CounterServlet.COUNT_HEADER).getValue()));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }
}
