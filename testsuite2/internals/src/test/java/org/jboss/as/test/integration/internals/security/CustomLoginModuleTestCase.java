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

package org.jboss.as.test.integration.internals.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.internals.websecurity.SecuredServlet;
import org.jboss.as.test.integration.internals.websecurity.WebSecurityPasswordBasedBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for custom login modules in authentication.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomLoginModuleTestCase {

    protected final String URL = "http://localhost:8080/" + getContextPath() + "/secured/";

    /**
     * Base method to create a {@link WebArchive}
     *
     * @param name Name of the war file
     * @param servletClass a class that is the servlet
     * @param webxml {@link URL} to the web.xml. This can be null
     * @return
     */
    public static WebArchive create(String name, Class<?> servletClass, URL webxml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, name);
        war.addClass(servletClass);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        war.addAsWebResource(tccl.getResource("web-secure.war/login.jsp"), "login.jsp");
        war.addAsWebResource(tccl.getResource("web-secure.war/error.jsp"), "error.jsp");
        war.addAsWebInfResource(tccl.getResource("custom-login-module.war/jboss-web.xml"), "jboss-web.xml");
        war.addClass(CustomTestLoginModule.class);

        if (webxml != null) {
            war.setWebXML(webxml);
        }

        return war;
    }

    @Deployment
    public static WebArchive deployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            // create required security domains
            createSecurityDomains(client);
        } catch (Exception e) {
            // ignore
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL webxml = tccl.getResource("web-secure.war/web.xml");
        WebArchive war = create("custom-login-module.war", SecuredServlet.class, webxml);
        WebSecurityPasswordBasedBase.printWar(war);
        return war;
    }

    @AfterClass
    public static void after() throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        // remove test security domains
        removeSecurityDomains(client);
    }

    @Test
    public void testSucessfulAuth() throws Exception {
        makeCall("anil", "anil", 200);
    }

    @Test
    public void testUnsucessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(URL);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            if (entity != null)
                entity.consumeContent();

            // We should get the Login Page
            StatusLine statusLine = response.getStatusLine();
            System.out.println("Login form get: " + statusLine);
            assertEquals(200, statusLine.getStatusCode());

            System.out.println("Initial set of cookies:");
            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }

            // We should now login with the user name and password
            HttpPost httpost = new HttpPost(URL + "/j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", pass));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            response = httpclient.execute(httpost);
            entity = response.getEntity();
            if (entity != null)
                entity.consumeContent();

            statusLine = response.getStatusLine();

            // Post authentication - we have a 302
            assertEquals(302, statusLine.getStatusCode());
            Header locationHeader = response.getFirstHeader("Location");
            String location = locationHeader.getValue();

            HttpGet httpGet = new HttpGet(location);
            response = httpclient.execute(httpGet);

            entity = response.getEntity();
            if (entity != null)
                entity.consumeContent();

            System.out.println("Post logon cookies:");
            cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }

            // Either the authentication passed or failed based on the expected status code
            statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    public static void createSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "custom-login-module");
        ModelNode loginModule = op.get(AUTHENTICATION).add();
        loginModule.get(CODE).set("org.jboss.as.test.integration.internals.security.CustomTestLoginModule");
        loginModule.get(FLAG).set("required");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "custom-login-module");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(OperationBuilder.Factory.create(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    public String getContextPath() {
        return "custom-login-module";
    }

}
