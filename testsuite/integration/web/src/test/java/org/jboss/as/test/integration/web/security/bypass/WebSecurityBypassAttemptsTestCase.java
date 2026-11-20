/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.bypass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.Test;

/**
 * Unit Test for the BASIC authentication and attempts to bypass it.
 *
 * @author Richard Opalka
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityBypassAttemptsTestCase {
    private static final Logger log = Logger.getLogger(WebSecurityBypassAttemptsTestCase.class);

    private static final String JBOSS_WEB_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<jboss-web>\n" +
            "    <security-domain>" + WebTestsSecurityDomainSetup.WEB_SECURITY_DOMAIN + "</security-domain>\n" +
            "</jboss-web>";

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClass(SecuredServlet.class);
        war.addAsWebInfResource(new StringAsset(JBOSS_WEB_CONTENT), "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityBypassAttemptsTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private void makeCall(String url, String user, String pass, int expectedStatusCode) throws Exception {
        CredentialsProvider credentialsProvider = null;
        HttpClientBuilder builder = HttpClients.custom();
        if (user != null && pass != null) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(this.url.getHost(), this.url.getPort()),
                    new UsernamePasswordCredentials(user, pass));
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        try (CloseableHttpClient httpclient = builder.build()) {
            HttpGet httpget = new HttpGet(url);

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            if (entity != null) {
                log.trace("Response content length: " + entity.getContentLength());
            }
            assertEquals(expectedStatusCode, statusLine.getStatusCode());

            if (200 == expectedStatusCode) {
                // check only in case authentication was successfull
                checkResponsecontent(EntityUtils.toString(entity), user, pass);
            }
            EntityUtils.consume(entity);
        }
    }

    private String getAppRootUrl() {
        return url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf("/"));
    }

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCall(getAppRootUrl() + "/secured/", "anil", "anil", 200);
    }

    /**
     * Test with user "marcus" who has the right password but does not have the right role
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall(getAppRootUrl() + "/secured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt1() throws Exception {
        makeCall(getAppRootUrl() + "/secured/a/../secured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt2() throws Exception {
        makeCall(getAppRootUrl() + "//secured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt3() throws Exception {
        makeCall(getAppRootUrl() + "/./secured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt4() throws Exception {
        makeCall(getAppRootUrl() + "/%2e/secured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt5() throws Exception {
        makeCall(getAppRootUrl() + "/%73ecured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt6() throws Exception {
        makeCall(getAppRootUrl() + "/s%65cured/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt7() throws Exception {
        makeCall(getAppRootUrl() + "/secured;/", "marcus", "marcus", 403);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt8() throws Exception {
        makeCall(getAppRootUrl() + "/secured%2f..%2fsecured/", "marcus", "marcus", 404);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt9() throws Exception {
        makeCall(getAppRootUrl() + "/%2fsecured/", "marcus", "marcus", 404);
    }

    /**
     * Test with user "marcus" who has the right password but wrong role - attempt to bypass the sec check via url magic
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt10() throws Exception {
        makeCall(getAppRootUrl() + "/%252fsecured/", "marcus", "marcus", 404);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt11() throws Exception {
        makeCall(getAppRootUrl() + "/secured/a/../secured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt12() throws Exception {
        makeCall(getAppRootUrl() + "//secured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt13() throws Exception {
        makeCall(getAppRootUrl() + "/./secured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt14() throws Exception {
        makeCall(getAppRootUrl() + "/%2e/secured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt15() throws Exception {
        makeCall(getAppRootUrl() + "/%73ecured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt16() throws Exception {
        makeCall(getAppRootUrl() + "/s%65cured/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt17() throws Exception {
        makeCall(getAppRootUrl() + "/secured;/", null, null, 401);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt18() throws Exception {
        makeCall(getAppRootUrl() + "/secured%2f..%2fsecured/", null, null, 404);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt19() throws Exception {
        makeCall(getAppRootUrl() + "/%2fsecured/", null, null, 404);
    }

    /**
     * Attempt to bypass the sec check via url magic - no credentials provided
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth_bypassAttempt20() throws Exception {
        makeCall(getAppRootUrl() + "/%252fsecured/", null, null, 404);
    }

    private void checkResponsecontent(String response, String user, String password) {
        assertNotNull("Response is 'null', we expected non-null response!", response);
        assertTrue("Remote user different from what we expected!", response.contains("Remote user: " + user));
        assertTrue("User principal different from what we expected!", response.contains("User principal: " + password));
        assertTrue("Authentication type different from what we expected!", response.contains("Authentication type: " +
                "BASIC"));
    }
}