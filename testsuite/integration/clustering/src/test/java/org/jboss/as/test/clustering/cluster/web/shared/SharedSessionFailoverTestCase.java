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
package org.jboss.as.test.clustering.cluster.web.shared;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that web applications within an ear can share sessions if configured appropriately.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SharedSessionFailoverTestCase extends ClusterAbstractTestCase {

    private static final String MODULE = "shared";
    private static final String MODULE_1 = "war1";
    private static final String MODULE_2 = "war2";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar");
        jar.addClass(Mutable.class);
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, MODULE_1 + ".war");
        war1.addClass(SimpleServlet.class);
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, MODULE_2 + ".war");
        war2.addClass(SimpleServlet.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, MODULE + ".ear");
        ear.addAsLibraries(jar);
        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsManifestResource(SharedSessionFailoverTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        return ear;
    }

    @Test
    public void test(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2
            ) throws MalformedURLException, URISyntaxException, IOException {

        URI uri11 = SimpleServlet.createURI(baseURL1.toURI().resolve(MODULE_1).toURL());
        URI uri12 = SimpleServlet.createURI(baseURL1.toURI().resolve(MODULE_2).toURL());
        URI uri21 = SimpleServlet.createURI(baseURL2.toURI().resolve(MODULE_1).toURL());
        URI uri22 = SimpleServlet.createURI(baseURL2.toURI().resolve(MODULE_2).toURL());

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();
        try {
            HttpResponse response = client.execute(new HttpGet(uri11));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri12));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri21));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri22));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }
}
