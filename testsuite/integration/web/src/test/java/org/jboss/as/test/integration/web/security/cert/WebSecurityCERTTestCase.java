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

package org.jboss.as.test.integration.web.security.cert;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.WebCERTTestsSecurityDomainSetup;
import org.jboss.security.JBossJSSESecurityDomain;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit test for CLIENT-CERT authentication.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebCERTTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityCERTTestCase {

    @ArquillianResource
    private ManagementClient mgmtClient;

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-client-cert.war");
        war.addClass(SecuredServlet.class);

        war.addAsWebInfResource(WebSecurityCERTTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityCERTTestCase.class.getPackage(), "web.xml", "web.xml");

        war.addAsResource(WebSecurityCERTTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(WebSecurityCERTTestCase.class.getPackage(), "roles.properties", "roles.properties");

        return war;
    }

    private static CloseableHttpClient getHttpsClient(String alias) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            JBossJSSESecurityDomain jsseSecurityDomain = new JBossJSSESecurityDomain("client-cert");
            jsseSecurityDomain.setKeyStorePassword("changeit");
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            URL keystore = tccl.getResource("security/client.keystore");
            jsseSecurityDomain.setKeyStoreURL(keystore.getPath());
            jsseSecurityDomain.setClientAlias(alias);
            jsseSecurityDomain.reloadKeyAndTrustStore();
            KeyManager[] keyManagers = jsseSecurityDomain.getKeyManagers();
            TrustManager[] trustManagers = jsseSecurityDomain.getTrustManagers();
            ctx.init(keyManagers, trustManagers, null);
            HostnameVerifier verifier = (string, ssls) -> true;
            SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(ctx, verifier);//SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", ssf)
                    .build();

            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

            /*SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 8380, ssf));
            return new DefaultHttpClient(ccm, base.getParams());*/


            return HttpClientBuilder.create()
                    .setSSLSocketFactory(ssf)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .setConnectionManager(ccm)
                    .build();


        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Test
    public void testClientCertSuccessfulAuth() throws Exception {
        makeCall("test", 200);
    }

    @Test
    public void testClientCertUnsuccessfulAuth() throws Exception {
        makeCall("test2", 403);
    }

    protected void makeCall(String alias, int expectedStatusCode) throws Exception {
        try (CloseableHttpClient httpclient = getHttpsClient(alias)) {
            HttpGet httpget = new HttpGet("https://" + mgmtClient.getMgmtAddress() + ":8380/web-secure-client-cert/secured/");
            HttpResponse response = httpclient.execute(httpget);

            StatusLine statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        }
    }
}
