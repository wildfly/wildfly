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
package org.jboss.as.test.integration.web.security.jaspi;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.basic.WebSecurityBASICTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests that JASPI authentication reports error if the authentication process fails.
 * <p>
 * The AuthModule always throws Exception, but doesn't set the http error code.
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebJaspiTestsSecurityDomainSetup.WithFailingAuthModule.class)
@Category(CommonCriteria.class)
public class WebSecurityJaspiWithFailingAuthModuleTestCase {

    private static final String JBOSS_WEB_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<jboss-web>\n" +
            "    <security-domain>" + WebJaspiTestsSecurityDomainSetup.WEB_SECURITY_DOMAIN + "</security-domain>\n" +
            "</jboss-web>";

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-jaspi.war");
        war.addClass(SecuredServlet.class);
        war.addClass(FailingAuthModule.class);

        war.addAsWebInfResource(new StringAsset(JBOSS_WEB_CONTENT), "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityBASICTestCase.class.getPackage(), "web.xml", "web.xml");

        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testShouldReturnErrorCodeIfTheAuthModuleThrowsException() throws Exception {
        makeCall("anil", "anil", 401);
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(user, pass));

        try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "secured/");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
            EntityUtils.consume(entity);
        }
    }
}