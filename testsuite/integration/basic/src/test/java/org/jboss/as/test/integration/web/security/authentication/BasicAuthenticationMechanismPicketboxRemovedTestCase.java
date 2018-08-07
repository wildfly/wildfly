/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.security.authentication;

import org.apache.http.HttpResponse;
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
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.web.security.authentication.deployment.SecuredEJB;
import org.jboss.as.test.integration.web.security.authentication.deployment.SecuredEJBServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Automated test for [ WFLY-10671 ] - tests tracker for picketbox subsystem removing.
 *
 * [ WFLY-10671 ] is a tracker for issues:
 * [ WFLY-10282 ] Test Enables elytron and removes security subsystem. After secured EJB is deployed and accessed using basic authorization test checks if correct response is returned after the EJB is called from the secured servlet.
 * [ WFLY-10292 ] After switching to elytron and removing picketbox subsystem DefaultJMSConnectionFactory is not found during server startup.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(BasicAuthMechanismServerSetupTask.class)
@RunAsClient
public class BasicAuthenticationMechanismPicketboxRemovedTestCase {

    private static final String USER = "user1";
    private static final String PASSWORD = "password1";
    private static final String EJB_SECURITY = "ejb-security";

    @Deployment(name = EJB_SECURITY)
    public static WebArchive appDeployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, EJB_SECURITY + ".war");
        war.addClasses(BasicAuthenticationMechanismPicketboxRemovedTestCase.class, SecuredEJB.class, SecuredEJBServlet.class);
        war.addAsWebInfResource(BasicAuthenticationMechanismPicketboxRemovedTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(BasicAuthenticationMechanismPicketboxRemovedTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    /**
     * Test checks if correct response is returned after the EJB is called from the secured servlet.
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void test(@ArquillianResource URL url) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(USER, PASSWORD));
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "SecuredEJBServlet/");
            HttpResponse response = httpclient.execute(httpget);
            assertNotNull("Response is 'null', we expected non-null response!", response);
            String text = Utils.getContent(response);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertTrue("User principal different from what we expected!", text.contains("Principal: " + USER));
            assertTrue("Remote user different from what we expected!", text.contains("Remote User: " + USER));
            assertTrue("Authentication type different from what we expected!", text.contains("Authentication Type: BASIC"));
        }
    }
}