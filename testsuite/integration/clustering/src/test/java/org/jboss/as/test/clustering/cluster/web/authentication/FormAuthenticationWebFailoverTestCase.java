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
package org.jboss.as.test.clustering.cluster.web.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a user remains authenticated following failover when using FORM authentication.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(WebSecurityDomainSetup.class)
public class FormAuthenticationWebFailoverTestCase extends AbstractClusteringTestCase {

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
        WebArchive war = ShrinkWrap.create(WebArchive.class, "form-authentication.war");
        war.addClass(SecureServlet.class);
        war.setWebXML(SecureServlet.class.getPackage(), "web-form.xml");
        war.addAsWebInfResource(SecureServlet.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsResource(SecureServlet.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(SecureServlet.class.getPackage(), "roles.properties", "roles.properties");
        war.addAsWebResource(SecureServlet.class.getPackage(), "login.html", "login.html");
        war.addAsWebResource(SecureServlet.class.getPackage(), "error.html", "error.html");
        return war;
    }

    @Test
    public void test(
            @ArquillianResource(SecureServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SecureServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        URI uri1 = SecureServlet.createURI(baseURL1);
        URI uri2 = SecureServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNull(response.getFirstHeader(SecureServlet.SESSION_ID_HEADER));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            HttpPost login = new HttpPost(baseURL1.toURI().resolve("j_security_check"));

            List<NameValuePair> pairs = new ArrayList<>(2);
            pairs.add(new BasicNameValuePair("j_username", "allowed"));
            pairs.add(new BasicNameValuePair("j_password", "password"));

            login.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
            response = client.execute(login);
            try {
                Assert.assertEquals(HttpServletResponse.SC_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            String sessionId = null;
            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNotNull(response.getFirstHeader(SecureServlet.SESSION_ID_HEADER));
                sessionId = response.getFirstHeader(SecureServlet.SESSION_ID_HEADER).getValue();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            undeploy(DEPLOYMENT_1);

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId, response.getFirstHeader(SecureServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            deploy(DEPLOYMENT_1);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId, response.getFirstHeader(SecureServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
