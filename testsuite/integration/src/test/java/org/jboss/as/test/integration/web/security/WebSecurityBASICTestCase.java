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
package org.jboss.as.test.integration.web.security;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Unit Test the BASIC authentication
 *
 * @author Anil Saldhana
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebSecurityBASICTestCase extends WebSecurityPasswordBasedBase {

    @Deployment
    public static WebArchive deployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            // create required security domains
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL webxml = tccl.getResource("web-secure-basic.war/web.xml");
        WebArchive war = WebSecurityPasswordBasedBase.create("web-secure-basic.war", SecuredServlet.class, true, webxml);
        war.addAsWebInfResource("web-secure-basic.war/jboss-web.xml", "jboss-web.xml");
        WebSecurityPasswordBasedBase.printWar(war);
        return war;
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope("localhost", 8080),
                    new UsernamePasswordCredentials(user, pass));

            HttpGet httpget = new HttpGet(URL);

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

    @Override
    public String getContextPath() {
        return "web-secure-basic";
    }
}