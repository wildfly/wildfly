/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.servlet3;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test the programmatic login feature of Servlet 3
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(WebSecurityProgrammaticLoginTestCase.SecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityProgrammaticLoginTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(testable = true)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-programmatic-login.war");
        war.addAsResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "roles.properties", "roles.properties");
        war.addAsManifestResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        war.addAsWebInfResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addClass(LoginServlet.class);
        war.addClass(SecuredServlet.class);
        war.addClass(AbstractSecurityDomainSetup.class);
        war.addPackage(CommonCriteria.class.getPackage());

        return war;
    }

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCall("anil", "anil", 200);
    }

    /**
     * <p>
     * Test with user "marcus" who has the right password but does not have the right role
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            // test hitting programmatic login servlet
            HttpGet httpget = new HttpGet(managementClient.getWebUri() + "/" + getContextPath() + "/login/?username=" + user + "&password=" + pass);

            System.out.println("executing request" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            System.out.println("----------------------------------------");
            StatusLine statusLine = response.getStatusLine();
            System.out.println(statusLine);
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
            }
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
            EntityUtils.consume(entity);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    public String getContextPath() {
        return "web-secure-programmatic-login";
    }

    static class SecurityDomainSetup extends AbstractSecurityDomainSetup {

        @Override
        protected String getSecurityDomainName() {
            return "web-programmatic-login";
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) {
            final ModelNode compositeOp = new ModelNode();
                    compositeOp.get(OP).set(COMPOSITE);
                    compositeOp.get(OP_ADDR).setEmptyList();

                    ModelNode steps = compositeOp.get(STEPS);
            String securityDomain = "web-programmatic-login";

            PathAddress address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, securityDomain);

            steps.add(Util.createAddOperation(address));
            address = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
            steps.add(Util.createAddOperation(address));

            ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, "UsersRoles"));


            loginModule.get(CODE).set("UsersRoles");
            loginModule.get(FLAG).set("required");
            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(loginModule);
            applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
        }
    }

}
