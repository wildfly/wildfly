/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.authentication.custom.CustomAuthenticationMechanism;
import org.jboss.as.test.clustering.cluster.web.authentication.custom.LogoutServlet;
import org.jboss.as.test.clustering.cluster.web.authentication.custom.SecuredServlet;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Validates that a user remains authenticated following failover when using FORM authentication.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup({ CustomAuthenticationWebFailoverTestCase.ElytronDomainSetupOverride.class, CustomAuthenticationWebFailoverTestCase.ServletElytronDomainSetupOverride.class, CustomAuthenticationWebFailoverTestCase.ServerSetup.class })
public class CustomAuthenticationWebFailoverTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = CustomAuthenticationWebFailoverTestCase.class.getSimpleName();
    private static final String SECURITY_DOMAIN_NAME = "authentication";

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
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(SecuredServlet.class.getPackage());
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        war.addAsWebInfResource("beans.xml", "beans.xml");
        war.addAsWebInfResource(CustomAuthenticationWebFailoverTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(CustomAuthenticationWebFailoverTestCase.class.getPackage(), "distributable-web.xml", "distributable-web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain"), new ElytronPermission("authenticate")), "permissions.xml");
        return war;
    }

    @Test
    public void test(
            @ArquillianResource(SecuredServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SecuredServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        URI uri1 = SecuredServlet.createURI(baseURL1);
        URI uri2 = SecuredServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                Assert.assertEquals(CustomAuthenticationMechanism.CREDENTIAL_REQUIRED_MESSAGE, response.getFirstHeader(CustomAuthenticationMechanism.MESSAGE_HEADER).getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                Assert.assertEquals(CustomAuthenticationMechanism.CREDENTIAL_REQUIRED_MESSAGE, response.getFirstHeader(CustomAuthenticationMechanism.MESSAGE_HEADER).getValue());
            }

            HttpUriRequest request = new HttpGet(uri1);
            // Validate login with incorrect credentials
            request.setHeader(CustomAuthenticationMechanism.USERNAME_HEADER, "allowed");
            request.setHeader(CustomAuthenticationMechanism.PASSWORD_HEADER, "wrong");

            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                Assert.assertEquals(CustomAuthenticationMechanism.INVALID_CREDENTIAL_MESSAGE, response.getFirstHeader(CustomAuthenticationMechanism.MESSAGE_HEADER).getValue());
            }

            // Login as unauthorized user
            request.setHeader(CustomAuthenticationMechanism.USERNAME_HEADER, "forbidden");
            request.setHeader(CustomAuthenticationMechanism.PASSWORD_HEADER, "password");

            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
            }

            // Logout so we can login again as an authorized user
            try (CloseableHttpResponse response = client.execute(new HttpGet(LogoutServlet.createURI(baseURL1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            // Validate login with correct credentials
            request.setHeader(CustomAuthenticationMechanism.USERNAME_HEADER, "allowed");
            request.setHeader(CustomAuthenticationMechanism.PASSWORD_HEADER, "password");

            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNotNull(response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER));
                Assert.assertEquals("allowed", response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER).getValue());
            }

            // Validate that subsequent request is already authenticated
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNotNull(response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER));
                Assert.assertEquals("allowed", response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER).getValue());
            }

            undeploy(DEPLOYMENT_1);

            // Validate that failover request is auto-authenticated
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNotNull(response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER));
                Assert.assertEquals("allowed", response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER).getValue());
            }

            deploy(DEPLOYMENT_1);

            // Validate that failback request is auto-authenticated
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertNotNull(response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER));
                Assert.assertEquals("allowed", response.getFirstHeader(SecuredServlet.PRINCIPAL_HEADER).getValue());
            }

            // Logout
            try (CloseableHttpResponse response = client.execute(new HttpGet(LogoutServlet.createURI(baseURL1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            // Verify that we are no longer authenticated on either server
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                Assert.assertEquals(CustomAuthenticationMechanism.CREDENTIAL_REQUIRED_MESSAGE, response.getFirstHeader(CustomAuthenticationMechanism.MESSAGE_HEADER).getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                Assert.assertEquals(CustomAuthenticationMechanism.CREDENTIAL_REQUIRED_MESSAGE, response.getFirstHeader(CustomAuthenticationMechanism.MESSAGE_HEADER).getValue());
            }
        }
    }

    static class ElytronDomainSetupOverride extends ElytronDomainServerSetupTask {

        public ElytronDomainSetupOverride() {
            super(SECURITY_DOMAIN_NAME);
        }
    }

    static class ServletElytronDomainSetupOverride extends ServletElytronDomainSetup {

        protected ServletElytronDomainSetupOverride() {
            super(SECURITY_DOMAIN_NAME, false);
        }
    }

    static class ServerSetup extends ManagementServerSetupTask {

        ServerSetup() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/policy=jacc:add(jacc-policy={})")
                            .add("/subsystem=undertow/application-security-domain=%s:write-attribute(name=integrated-jaspi, value=false)", SECURITY_DOMAIN_NAME)
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/policy=jacc:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}