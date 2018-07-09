/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.security.jaspi;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Test configures JASPI in security subsystem and sends HTTP request to the deployed web application with the FORM authentication.
 * Test for [ WFLY-10533 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup({ JaspiSimpleServerLoginDomainSetup.class })
@RunAsClient
public class JaspiFormAuthTestCase {

    private static final String DEPLOYMENT = "test";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addAsWebInfResource(JaspiFormAuthTestCase.class.getPackage(), "jaspiDeployment/web.xml", "/web.xml");
        war.addAsWebInfResource(JaspiFormAuthTestCase.class.getPackage(), "jaspiDeployment/jboss-web.xml", "jboss-web.xml");
        war.addAsWebResource(JaspiFormAuthTestCase.class.getPackage(), "jaspiDeployment/login.html", "login.html");
        war.addAsWebResource(JaspiFormAuthTestCase.class.getPackage(), "jaspiDeployment/error.jsp", "error.jsp");
        return war;
    }

    @Test
    public void testJaspiFormAuth(@ArquillianResource URL url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final HttpGet httpGet = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/test/");
            HttpResponse response = httpClient.execute(httpGet);
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }
}
