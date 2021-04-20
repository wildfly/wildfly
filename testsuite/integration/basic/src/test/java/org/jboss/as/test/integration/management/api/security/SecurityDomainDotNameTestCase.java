/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.security;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Creates security domain with a name containing dots and tries to log in using the POST method through the secured form.
 *
 * Test for [ WFLY-14692 ].
 *
 * @author Daniel Cihak
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SecurityDomainDotNameServerSetupTask.class)
public class SecurityDomainDotNameTestCase extends ContainerResourceMgmtTestBase {

    private static Logger log = Logger.getLogger(SecurityDomainDotNameTestCase.class);

    @ArquillianResource
    private URL baseURLNoAuth;

    DefaultHttpClient httpclient = new DefaultHttpClient();
    private static final String DEPLOYMENT = "secured-login";

    @Deployment(name=DEPLOYMENT, testable = false)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SecurityDomainDotNameTestCase.war");
        war.addClass(SecurityDomainDotNameTestCase.class);
        war.addAsWebInfResource(SecurityDomainDotNameTestCase.class.getPackage(), "web.xml", "/web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>test.xyz.security-domain.default</security-domain></jboss-web>"), "jboss-web.xml");
        war.addAsWebResource(SecurityDomainDotNameTestCase.class.getPackage(), "restricted/login.html", "restricted/login.html");
        war.addAsWebResource(SecurityDomainDotNameTestCase.class.getPackage(), "restricted/errors.jsp", "restricted/errors.jsp");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testDotNameSecurityDomain() throws Exception {

        // Submit the login form
        HttpPost formPost = new HttpPost(baseURLNoAuth + "j_security_check");
        formPost.addHeader("Referer", baseURLNoAuth + "restricted/login.html");

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("j_username", "user1"));
        formparams.add(new BasicNameValuePair("j_password", "password1"));
        formPost.setEntity(new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8));

        log.info("Executing request " + formPost.getRequestLine());
        HttpResponse postResponse = httpclient.execute(formPost);

        int statusCode = postResponse.getStatusLine().getStatusCode();
        Header[] errorHeaders = postResponse.getHeaders("X-NoJException");
        assertTrue("Should see HTTP_OK. Got " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        EntityUtils.consume(postResponse.getEntity());
    }
}
