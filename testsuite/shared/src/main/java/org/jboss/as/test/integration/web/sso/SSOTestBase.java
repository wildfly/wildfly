/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Base class for tests of web app single sign-on
 *
 * @author Brian Stansberry
 * @author lbarreiro@redhat.com
 */
public final class SSOTestBase {

    public static final String USERNAME = "user1";
    public static final String PASSWORD = "password1";
    public static final String USERNAME_2 = "user2";
    public static final String PASSWORD_2 = "password2";
    public static final String ROLE = "Users";

    /*
     * Nothing extends this class or needs to instantiate it, instead publicstatic accessor methods are used.
     */
    private SSOTestBase() {}

    /**
     * Test single sign-on across two web apps using form based auth
     *
     * @throws Exception
     */
    public static void executeFormAuthSingleSignOnTest(URL serverA, URL serverB, Logger log) throws Exception {
        URL warA1 = new URL(serverA, "/war1/");
        URL warB2 = new URL(serverB, "/war2/");

        // Start by accessing the secured index.html of war1
        CookieStore store = new BasicCookieStore();
        HttpClient httpclient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .disableRedirectHandling()
                .build();
        try {
            checkAccessDenied(httpclient, warA1 + "index.html");

            log.debug("Saw JSESSIONID=" + getSessionIdValueFromState(store));

            // Submit the login form
            executeFormLogin(httpclient, warA1);

            String ssoID = processSSOCookie(store, serverA.toString(), serverB.toString());
            log.debug("Saw JSESSIONIDSSO=" + ssoID);

            // Now try getting the war2 index using the JSESSIONIDSSO cookie
            log.debug("Prepare /war2/index.html get");
            checkAccessAllowed(httpclient, warB2 + "index.html");

            // Access a secured servlet that calls a secured Jakarta Enterprise Beans in war2 to test
            // propagation of the SSO identity to the Jakarta Enterprise Beans container.
            checkAccessAllowed(httpclient, warB2 + "NotAnEJBServlet");

            // Now try logging out of war2
            executeLogout(httpclient, warB2);
        } finally {
            HttpClientUtils.closeQuietly(httpclient);
        }

        try {
            // Reset Http client
            httpclient = HttpClients.createDefault();

            // Try accessing war1 again
            checkAccessDenied(httpclient, warA1 + "index.html");

            // Try accessing war2 again
            checkAccessDenied(httpclient, warB2 + "index.html");
        } finally {
            HttpClientUtils.closeQuietly(httpclient);
        }
    }

    public static void executeNoAuthSingleSignOnTest(URL serverA, URL serverB, Logger log) throws Exception {
        URL warA1 = new URL(serverA, "/war1/");
        URL warB2 = new URL(serverB + "/war2/");
        URL warB6 = new URL(serverB + "/war6/");

        // Start by accessing the secured index.html of war1
        CookieStore store = new BasicCookieStore();
        HttpClient httpclient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder().setDefaultCookieStore(store).build();
        try {
            checkAccessDenied(httpclient, warA1 + "index.html");

            log.debug("Saw JSESSIONID=" + getSessionIdValueFromState(store));

            // Submit the login form
            executeFormLogin(httpclient, warA1);

            String ssoID = processSSOCookie(store, serverA.toString(), serverB.toString());
            log.debug("Saw JSESSIONIDSSO=" + ssoID);

            // Now try getting the war2 index using the JSESSIONIDSSO cookie
            log.debug("Prepare /war2/index.html get");
            checkAccessAllowed(httpclient, warB2 + "index.html");

            // Access a secured servlet that calls a secured Jakarta Enterprise Beans in war2 to test
            // propagation of the SSO identity to the Jakarta Enterprise Beans container.
            checkAccessAllowed(httpclient, warB2 + "NotAnEJBServlet");

            // Do the same test on war6 to test SSO auth replication with no auth
            // configured war, by having no security domain this will map to Undertow's default
            // security domain of "other" so unless SSO is operational it should prompt to authenticate.
            checkAccessDenied(httpclient, warB6 + "index.html");

            checkAccessAllowed(httpclient, warB2 + "NotAnEJBServlet");
        } finally {
            HttpClientUtils.closeQuietly(httpclient);
        }
    }

    /**
     * Test single sign-on across two web apps using form based auth.
     *
     * Test that after session timeout SSO is destroyed.
     *
     * @throws Exception
     */
    public static void executeFormAuthSSOTimeoutTest(URL serverA, URL serverB, Logger log) throws Exception {
        URL warA1 = new URL(serverA, "/war1/");
        URL warB2 = new URL(serverB, "/war2/");

        // Start by accessing the secured index.html of war1
        CookieStore store = new BasicCookieStore();
        HttpClient httpclient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .disableRedirectHandling()
                .build();
        try {
            checkAccessDenied(httpclient, warA1 + "index.html");

            log.debug("Saw JSESSIONID=" + getSessionIdValueFromState(store));

            // Submit the login form
            executeFormLogin(httpclient, warA1);

            String ssoID = processSSOCookie(store, serverA.toString(), serverB.toString());
            log.debug("Saw JSESSIONIDSSO=" + ssoID);

            // After login I should still have access + set session timeout to 5 seconds
            checkAccessAllowed(httpclient, warA1 + "set_session_timeout.jsp");

            // Also access to war2 should be granted + set session timeout to 5 seconds
            checkAccessAllowed(httpclient, warB2 + "set_session_timeout.jsp");

            // wait 5 seconds session timeout + 1 seconds reserve
            Thread.sleep((5+1)*1000);

            // After timeout I should be not able to access the app
            checkAccessDenied(httpclient, warA1 + "index.html");
            checkAccessDenied(httpclient, warB2 + "index.html");

        } finally {
            HttpClientUtils.closeQuietly(httpclient);
        }

    }


    public static void executeLogout(HttpClient httpConn, URL warURL) throws IOException {
        HttpGet logout = new HttpGet(warURL + "Logout");
        HttpResponse response = httpConn.execute(logout);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertTrue("Logout: Didn't see code 302 (HTTP_MOVED_TEMP), but saw instead " + statusCode, statusCode == HttpURLConnection.HTTP_MOVED_TEMP);

            Header location = response.getFirstHeader("Location");
            assertTrue("Get of " + warURL + "Logout not redirected to login page", location.getValue().contains("index.html"));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static void checkAccessAllowed(HttpClient httpConn, String url) throws IOException {
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response = httpConn.execute(getMethod);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertTrue("Expected code == OK but got " + statusCode + " for request=" + url, statusCode == HttpURLConnection.HTTP_OK);

            String body = EntityUtils.toString(response.getEntity());
            assertTrue("Get of " + url + " redirected to login page", !body.contains("j_security_check"));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static void executeFormLogin(HttpClient httpConn, URL warURL) throws IOException {
        // Submit the login form
        HttpPost formPost = new HttpPost(warURL + "j_security_check");
        formPost.addHeader("Referer", warURL + "login.html");

        List<NameValuePair> formparams = new ArrayList<>();
        formparams.add(new BasicNameValuePair("j_username", "user1"));
        formparams.add(new BasicNameValuePair("j_password", "password1"));
        formPost.setEntity(new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8));

        HttpResponse postResponse = httpConn.execute(formPost);
        try {
            int statusCode = postResponse.getStatusLine().getStatusCode();
            Header[] errorHeaders = postResponse.getHeaders("X-NoJException");
            assertTrue("Should see HTTP_MOVED_TEMP. Got " + statusCode, statusCode == HttpURLConnection.HTTP_MOVED_TEMP);
            assertTrue("X-NoJException(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
            EntityUtils.consume(postResponse.getEntity());

            // Follow the redirect to the index.html page
            String indexURL = postResponse.getFirstHeader("Location").getValue();
            HttpGet rediretGet = new HttpGet(indexURL);
            HttpResponse redirectResponse = httpConn.execute(rediretGet);

            statusCode = redirectResponse.getStatusLine().getStatusCode();
            errorHeaders = redirectResponse.getHeaders("X-NoJException");
            assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
            assertTrue("X-NoJException(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);

            String body = EntityUtils.toString(redirectResponse.getEntity());
            assertTrue("Get of " + indexURL + " redirected to login page", !body.contains("j_security_check"));
        } finally {
            HttpClientUtils.closeQuietly(postResponse);
        }
    }

    public static void checkAccessDenied(HttpClient httpConn, String url) throws IOException {
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response = httpConn.execute(getMethod);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertTrue("Expected code == OK but got " + statusCode + " for request=" + url, statusCode == HttpURLConnection.HTTP_OK);

            String body = EntityUtils.toString(response.getEntity());
            assertTrue("Redirected to login page for request=" + url + ", body[" + body + "]", body.indexOf("j_security_check") > 0);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static String processSSOCookie(CookieStore cookieStore, String serverA, String serverB) {
        String ssoID = null;
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("JSESSIONIDSSO".equalsIgnoreCase(cookie.getName())) {
                ssoID = cookie.getValue();
                if (!serverA.equals(serverB)) {
                    // Make an sso cookie to send to serverB
                    Cookie copy = copyCookie(cookie, serverB);
                    cookieStore.addCookie(copy);
                }
            }
        }

        assertTrue("Didn't see JSESSIONIDSSO: " + cookieStore.getCookies(), ssoID != null);
        return ssoID;
    }

    public static Cookie copyCookie(Cookie toCopy, String targetServer) {
        // Parse the target server down to a domain name
        int index = targetServer.indexOf("://");
        if (index > -1) {
            targetServer = targetServer.substring(index + 3);
        }
        // JBAS-8540
        // need to be able to parse IPv6 URLs which have enclosing brackets
        // HttpClient 3.1 creates cookies which include the square brackets
        // index = targetServer.indexOf(":");
        index = targetServer.lastIndexOf(":");
        if (index > -1) {
            targetServer = targetServer.substring(0, index);
        }
        index = targetServer.indexOf("/");
        if (index > -1) {
            targetServer = targetServer.substring(0, index);
        }

        // Cookie copy = new Cookie(targetServer, toCopy.getName(), toCopy.getValue(), "/", null, false);
        BasicClientCookie copy = new BasicClientCookie(toCopy.getName(), toCopy.getValue());
        copy.setDomain(targetServer);
        return copy;
    }

    public static String getSessionIdValueFromState(CookieStore cookieStore) {
        String sessionID = null;
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                sessionID = cookie.getValue();
                break;
            }
        }
        return sessionID;
    }

    public static WebArchive createSsoWar(String warName) {
        return createSsoWar(warName, true);
    }

    public static WebArchive createSsoWar(String warName, boolean includeJBossWeb) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
        war.setWebXML(tccl.getResource(resourcesLocation + "web-form-auth.xml"));
        if (includeJBossWeb) {
            war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        }

        war.addAsWebResource(tccl.getResource(resourcesLocation + "error.html"), "error.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.jsp"), "index.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "set_session_timeout.jsp"), "set_session_timeout.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "login.html"), "login.html");

        war.addClass(NotAnEJBServlet.class);
        war.addClass(LogoutServlet.class);

        return war;
    }

    public static EnterpriseArchive createSsoEar() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";

        WebArchive war1 = createSsoWar("sso-form-auth1.war");
        WebArchive war2 = createSsoWar("sso-form-auth2.war");
        WebArchive war3 = createSsoWar("sso-with-no-auth.war", false);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "web-sso.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application.xml"));

        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsModule(war3);

        return ear;
    }

}
