/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the situation when security manager is enabled and MappingResult is called in Servlet.
 *
 * RuntimePermission("getClassLoader") and RuntimePermission("createClassLoader") should NOT be necessary.
 * See: https://issues.jboss.org/browse/WFLY-8760
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebSimpleRoleMappingSecurityDomainSetup.class)
public class WebSecuritySimpleRoleMappingSecurityManagerTestCase {

    private static final String JBOSS_WEB_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<jboss-web>\n" +
            "    <security-domain>" + WebSimpleRoleMappingSecurityDomainSetup.WEB_SECURITY_DOMAIN + "</security-domain>\n" +
            "</jboss-web>";

    private static final String WEB_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
            "    version=\"3.0\">\n" +
            "    <login-config>\n" +
            "        <auth-method>BASIC</auth-method>\n" +
            "        <realm-name>WebSecurityBasic</realm-name>\n" +
            "    </login-config>\n" +
            "</web-app>";

    @ArquillianResource
    private URL url;

    private static WebArchive prepareDeployment(final String jbossWebFileName) throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure.war");
        war.addClasses(RoleMappingSecuredServlet.class);
        war.addAsWebInfResource(new StringAsset(JBOSS_WEB_CONTENT), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(WEB_CONTENT), "web.xml");

        return war;
    }

    @Deployment(testable = false)
    public static WebArchive deployment() throws Exception {
        WebArchive war = prepareDeployment("jboss-web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("org.picketbox.factories.SecurityFactory.*"),
                new RuntimePermission("org.jboss.security.*"),
                new javax.security.auth.AuthPermission("getLoginConfiguration")
                ),
                "permissions.xml");
        return war;
    }

    /**
     * At this time peter can go through because he has role mapped in the map-module option.
     *
     * @throws Exception
     */
    @Test
    public void testPrincipalMappingOnRole() throws Exception {
        makeCall("peter", "peter", 200, "gooduser:superuser");
    }

    private void makeCall(String user, String pass, int expectedCode, String expectedOut) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(user, pass));
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "rolemapping-secured/");
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            assertEquals(expectedCode, statusLine.getStatusCode());
            InputStream input = entity.getContent();
            assertEquals(expectedOut, new String(IOUtil.asByteArray(input)));
            input.close();
        }
    }

}