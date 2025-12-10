/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.as.test.http.util.TestHttpClientUtils;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.web.formauth.LogoutServlet;
import org.jboss.as.test.integration.web.formauth.SecureServlet;
import org.jboss.as.test.integration.web.formauth.FormAuthUnitTestCase;
import org.jboss.as.test.integration.web.formauth.SecuredPostServlet;
import org.jboss.as.test.integration.web.sharedsession.SharedSessionTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests a full SSO re-authentication flow. This test verifies:
 * 1. An SSO session for 'user1' successfully bypasses the FORM login page of a second application ('app-2').
 * 2. A subsequent POST to j_security_check on 'app-2' with 'user2' credentials successfully switches the session's identity.
 * 3. The new 'user2' identity is correctly replicated back to the first application ('app-1') via the updated SSO session.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SingleSignOnSetup.class)
public class SSOFormReAuthenticationTestCase {

    private static final String MODULE_NAME = SSOFormReAuthenticationTestCase.class.getSimpleName();
    private static final String LOGIN_APP_CONTEXT = MODULE_NAME + "-login-app";
    private static final String FORM_APP_CONTEXT = MODULE_NAME + "-form-app";
    public static final String RESTRICTED_SECURED_SERVLET = "restricted/SecuredServlet";

    @ArquillianResource
    private URL baseURL;

    @Deployment(name = "SSOFormReAuthenticationTestCase.ear", testable = false)
    public static EnterpriseArchive deployment() {

        JavaArchive appClasses = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addClass(SecureServlet.class)
                .addClass(SecuredPostServlet.class)
                .addClass(LogoutServlet.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, MODULE_NAME + ".ear")
                .addAsModule(createFormAuthWar(LOGIN_APP_CONTEXT + ".war", appClasses))
                .addAsModule(createFormAuthWar(FORM_APP_CONTEXT + ".war", appClasses))
                .addAsManifestResource(SharedSessionTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");

        return ear;
    }

    @Test
    @OperateOnDeployment("SSOFormReAuthenticationTestCase.ear")
    public void testFormAuthBypassAndReAuthentication() throws Exception {

        URL loginAppUrl = new URL(baseURL, LOGIN_APP_CONTEXT + "/");
        URL formAppUrl = new URL(baseURL, FORM_APP_CONTEXT + "/");

        // We need the direct URLs to the secured servlets for our checks
        String loginAppSecuredUrl = new URL(loginAppUrl, RESTRICTED_SECURED_SERVLET).toExternalForm();
        String formAppSecuredUrl = new URL(formAppUrl, RESTRICTED_SECURED_SERVLET).toExternalForm();

        CookieStore store = new BasicCookieStore();
        HttpClient client = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .build();

        try {
            // Authenticate against login-app to get SSO cookie as USER_1
            SSOTestBase.checkAccessDenied(client, loginAppSecuredUrl);
            SSOTestBase.executeFormLogin(client, loginAppUrl);
            String ssoCookieValue1 = SSOTestBase.processSSOCookie(store, baseURL.toString(), baseURL.toString());

            // Access form-app and assert SSO bypass
            HttpResponse response = client.execute(new HttpGet(formAppSecuredUrl));
            String body = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
            Assert.assertTrue("SSO bypass should log in as user1", body.contains("You have accessed this servlet as user:" + SSOTestBase.USERNAME));

            // Re-Authenticate on form-app as USER_2
            HttpPost formPost = new HttpPost(new URL(formAppUrl, "j_security_check").toExternalForm());
            formPost.addHeader("Referer", new URL(formAppUrl, "restricted/login.html").toExternalForm());
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair("j_username", SSOTestBase.USERNAME_2));
            formparams.add(new BasicNameValuePair("j_password", SSOTestBase.PASSWORD_2));
            formPost.setEntity(new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8));

            response = client.execute(formPost);
            Assert.assertEquals("Re-authentication POST should redirect. Got " + response.getStatusLine().getStatusCode(),
                    HttpURLConnection.HTTP_MOVED_TEMP, response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());

            // Verify Identity Switch on app-2
            response = client.execute(new HttpGet(formAppSecuredUrl));
            body = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
            Assert.assertTrue("Identity should be switched to user2 on app-2", body.contains("You have accessed this servlet as user:" + SSOTestBase.USERNAME_2));

            // Assert the SSO cookie was also updated
            String ssoCookieValue2 = SSOTestBase.processSSOCookie(store, baseURL.toString(), baseURL.toString());
            Assert.assertNotNull(ssoCookieValue2);
            Assert.assertNotEquals("SSO cookie should be new after re-authentication", ssoCookieValue1, ssoCookieValue2);

            // Verify Identity Replicated Back to app-1
            response = client.execute(new HttpGet(loginAppSecuredUrl));
            body = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
            Assert.assertTrue("Identity should be switched to user2 on app-1 as well", body.contains("You have accessed this servlet as user:" + SSOTestBase.USERNAME_2));

        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    /**
     * Private helper method to create a WAR configured for FORM auth
     * using the resources from FormAuthUnitTestCase.
     */
    private static WebArchive createFormAuthWar(String warName, JavaArchive appClasses) {
        String resourcesLocation = "org/jboss/as/test/integration/web/formauth/resources/";
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName)
                .addAsLibrary(appClasses)
                .addAsWebInfResource(FormAuthUnitTestCase.class.getPackage(), "resources/web.xml", "web.xml")
                .addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html")
                .addAsWebResource(tccl.getResource(resourcesLocation + "unsecure_form.html"), "unsecure_form.html")
                .addAsWebResource(tccl.getResource(resourcesLocation + "restricted/errors.jsp"), "restricted/errors.jsp")
                .addAsWebResource(tccl.getResource(resourcesLocation + "restricted/error.html"), "restricted/error.html")
                .addAsWebResource(tccl.getResource(resourcesLocation + "restricted/login.html"), "restricted/login.html")
                .addAsWebInfResource(new StringAsset("<jboss-web><security-domain>sso-domain</security-domain></jboss-web>"), "jboss-web.xml");

        return war;
    }
}