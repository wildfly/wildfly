/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.identity.propagation;

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
import org.jboss.as.test.integration.web.security.identity.propagation.deployment.Hello;
import org.jboss.as.test.integration.web.security.identity.propagation.deployment.HelloBean;
import org.jboss.as.test.integration.web.security.identity.propagation.deployment.IdentityPropagationServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test configures Elytron to use Identity Propagation.
 * Test deploys application with the secured servlet and checks if the identity sets by
 * HttpServletRequest.login() is propagated into the secured EJB.
 *
 * Test for [ WFLY-11787 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(IdentityPropagationServerSetupTask.class)
@RunAsClient
public class IdentityPropagationAuthenticationTestCase {

    private static final String DEPLOYMENT = "httpRequestLogin";
    public static final String USER = "user1";
    public static final String PASSWORD = "password1";

    @Deployment(name=DEPLOYMENT)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(HelloBean.class, Hello.class, IdentityPropagationServlet.class);
        war.addAsWebInfResource(IdentityPropagationAuthenticationTestCase.class.getPackage(), "deployment/web.xml", "web.xml");
        war.addAsWebInfResource(IdentityPropagationAuthenticationTestCase.class.getPackage(), "deployment/jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Test
    public void testIdentityPropagationAuthentication(@ArquillianResource URL url) throws Exception {
        HttpGet httpGet = new HttpGet(url.toExternalForm() + "IdentityPropagationServlet/");
        HttpResponse response = null;

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()), new UsernamePasswordCredentials(USER, PASSWORD));
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {
            response = httpclient.execute(httpGet);
        }

        assertNotNull("Response is 'null', we expected non-null response!", response);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }
}
