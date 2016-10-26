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

package org.jboss.as.test.integration.security.loginmodules;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.security.auth.AuthPermission;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.security.loginmodules.common.CustomTestLoginModule;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit test for custom login modules in authentication.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomLoginModuleTestCase.CustomLoginModuleSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class CustomLoginModuleTestCase {


    @ArquillianResource(SecuredServlet.class)
    URL deploymentURL;

    private String getURL() {
        return deploymentURL.toString() + "secured/";
    }

    static class CustomLoginModuleSecurityDomainSetup extends AbstractSecurityDomainSetup {

        @Override
        protected String getSecurityDomainName() {
            return "custom-login-module";
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op = new ModelNode();

            op.get(OP).set(COMPOSITE);
            op.get(OP_ADDR).setEmptyList();
            PathAddress address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, getSecurityDomainName());
            op.get(STEPS).add(Util.createAddOperation(address));


            address = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
            op.get(STEPS).add(Util.createAddOperation(address));

            ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, CustomTestLoginModule.class.getName()));
            loginModule.get(CODE).set(CustomTestLoginModule.class.getName());
            loginModule.get(FLAG).set("required");
            op.get(STEPS).add(loginModule);
            applyUpdates(managementClient.getControllerClient(), Arrays.asList(op));
        }
    }

    /**
     * Base method to create a {@link WebArchive}
     *
     * @param name         Name of the war file
     * @param servletClass a class that is the servlet
     * @return
     */
    public static WebArchive create(String name, Class<?> servletClass) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, name);
        war.addClass(servletClass);

        war.addAsWebResource(CustomLoginModuleTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsWebResource(CustomLoginModuleTestCase.class.getPackage(), "error.jsp", "error.jsp");
        war.addAsWebInfResource(CustomLoginModuleTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.setWebXML(CustomLoginModuleTestCase.class.getPackage(), "web.xml");
        war.addClass(CustomTestLoginModule.class);
        war.addAsManifestResource(createPermissionsXmlAsset(new AuthPermission("modifyPrincipals")), "permissions.xml");
        return war;
    }

    @Deployment
    public static WebArchive deployment() throws IOException {
        WebArchive war = create("custom-login-module.war", SecuredServlet.class);
        return war;
    }

    @Test
    public void testSuccessfulAuth() throws Exception {
        makeCall("anil", "anil", 200);
    }

    @Test
    public void testUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(getURL());

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            // We should get the Login Page
            StatusLine statusLine = response.getStatusLine();
            //System.out.println("Login form get: " + statusLine);
            assertEquals(200, statusLine.getStatusCode());

            /*System.out.println("Initial set of cookies:");
            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }*/

            // We should now login with the user name and password
            HttpPost httpost = new HttpPost(getURL() + "j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", pass));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            response = httpclient.execute(httpost);
            entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            statusLine = response.getStatusLine();

            // Post authentication - we have a 302
            assertEquals(302, statusLine.getStatusCode());
            Header locationHeader = response.getFirstHeader("Location");
            String location = locationHeader.getValue();

            HttpGet httpGet = new HttpGet(location);
            response = httpclient.execute(httpGet);

            entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            /*System.out.println("Post logon cookies:");
            cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }*/

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

    public String getContextPath() {
        return "custom-login-module";
    }

}
