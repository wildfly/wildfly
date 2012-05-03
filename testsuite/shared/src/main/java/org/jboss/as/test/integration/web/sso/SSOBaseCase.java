/*
 * JBoss, a division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.web.sso;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

/**
 * Base class for tests of web app single sign-on
 * 
 * @author Brian Stansberry
 * @author lbarreiro@redhat.com
 */
public abstract class SSOBaseCase {

    /**
     * Test single sign-on across two web apps using form based auth
     * 
     * @throws Exception
     */
    public static void executeFormAuthSingleSignOnTest(URL serverA, URL serverB, Logger log) throws Exception {
        URL warA1 = new URL(serverA, "/war1/");
        URL warB2 = new URL(serverB, "/war2/");

        // Start by accessing the secured index.html of war1
        DefaultHttpClient httpclient = new DefaultHttpClient();

        checkAccessDenied(httpclient, warA1 + "index.html");

        CookieStore store = httpclient.getCookieStore();

        log.debug("Saw JSESSIONID=" + getSessionIdValueFromState(store));

        // Submit the login form
        executeFormLogin(httpclient, warA1);

        String ssoID = processSSOCookie(store, serverA.toString(), serverB.toString());
        log.debug("Saw JSESSIONIDSSO=" + ssoID);


        // Now try getting the war2 index using the JSESSIONIDSSO cookie
        log.debug("Prepare /war2/index.html get");
        checkAccessAllowed(httpclient, warB2 + "index.html");

        // Access a secured servlet that calls a secured ejb in war2 to test
        // propagation of the SSO identity to the ejb container.
        checkAccessAllowed(httpclient, warB2 + "EJBServlet");

        // Now try logging out of war2
        executeLogout(httpclient, warB2);


        // Reset Http client
        httpclient = new DefaultHttpClient();

        // Try accessing war1 again
        checkAccessDenied(httpclient, warA1 + "index.html");

        // Try accessing war2 again
        checkAccessDenied(httpclient, warB2 + "index.html");

    }

    public static void executeNoAuthSingleSignOnTest(URL serverA, URL serverB, Logger log) throws Exception {
        URL warA1 = new URL(serverA, "/war1/");
        URL warB2 = new URL(serverB + "/war2/");
        URL warB6 = new URL(serverB + "/war6/");

        // Start by accessing the secured index.html of war1
        DefaultHttpClient httpclient = new DefaultHttpClient();

        checkAccessDenied(httpclient, warA1 + "index.html");

        CookieStore store = httpclient.getCookieStore();

        log.debug("Saw JSESSIONID=" + getSessionIdValueFromState(store));

        // Submit the login form
        executeFormLogin(httpclient, warA1);

        String ssoID = processSSOCookie(store, serverA.toString(), serverB.toString());
        log.debug("Saw JSESSIONIDSSO=" + ssoID);


        // Now try getting the war2 index using the JSESSIONIDSSO cookie
        log.debug("Prepare /war2/index.html get");
        checkAccessAllowed(httpclient, warB2 + "index.html");

        // Access a secured servlet that calls a secured ejb in war2 to test
        // propagation of the SSO identity to the ejb container.
        checkAccessAllowed(httpclient, warB2 + "EJBServlet");

        // Do the same test on war6 to test SSO auth replication with no auth
        // configured war
        checkAccessAllowed(httpclient, warB6 + "index.html");

        checkAccessAllowed(httpclient, warB2 + "EJBServlet");

    }

    public static void executeLogout(HttpClient httpConn, URL warURL) throws IOException {
        HttpGet logout = new HttpGet(warURL + "Logout");
        logout.setParams(new BasicHttpParams().setParameter("http.protocol.handle-redirects", false));
        HttpResponse response = httpConn.execute(logout);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Logout: Didn't saw HTTP_MOVED_TEMP(" + statusCode + ")", statusCode == HttpURLConnection.HTTP_MOVED_TEMP);

        Header location = response.getFirstHeader("Location");
        assertTrue("Get of " + warURL + "Logout not redirected to login page", location.getValue().indexOf("index.html") >= 0);
    }

    public static void checkAccessAllowed(HttpClient httpConn, String url) throws IOException {
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response = httpConn.execute(getMethod);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Expected code == OK but got " + statusCode + " for request=" + url, statusCode == HttpURLConnection.HTTP_OK);

        String body = EntityUtils.toString(response.getEntity());
        assertTrue("Get of " + url + " redirected to login page", body.indexOf("j_security_check") < 0);
    }

    public static void executeFormLogin(HttpClient httpConn, URL warURL) throws IOException {
        // Submit the login form
        HttpPost formPost = new HttpPost(warURL + "j_security_check");
        formPost.addHeader("Referer", warURL + "login.html");

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("j_username", "user1"));
        formparams.add(new BasicNameValuePair("j_password", "password1"));
        formPost.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));

        HttpResponse postResponse = httpConn.execute(formPost);

        int statusCode = postResponse.getStatusLine().getStatusCode();
        Header[] errorHeaders = postResponse.getHeaders("X-NoJException");
        assertTrue("Should see HTTP_MOVED_TEMP. Got " + statusCode, statusCode == HttpURLConnection.HTTP_MOVED_TEMP);
        assertTrue("X-NoJException(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
        EntityUtils.consume(postResponse.getEntity());

        // Follow the redirect to the index.html page
        String indexURL = postResponse.getFirstHeader("Location").getValue();
        HttpGet rediretGet = new HttpGet(indexURL.toString());
        HttpResponse redirectResponse = httpConn.execute(rediretGet);

        statusCode = redirectResponse.getStatusLine().getStatusCode();
        errorHeaders = redirectResponse.getHeaders("X-NoJException");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-NoJException(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);

        String body = EntityUtils.toString(redirectResponse.getEntity());
        assertTrue("Get of " + indexURL + " redirected to login page", body.indexOf("j_security_check") < 0);
    }

    public static void checkAccessDenied(HttpClient httpConn, String url) throws IOException {
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response = httpConn.execute(getMethod);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Expected code == OK but got " + statusCode + " for request=" + url, statusCode == HttpURLConnection.HTTP_OK);

        String body = EntityUtils.toString(response.getEntity());
        assertTrue("Redirected to login page for request=" + url + ", body[" + body + "]", body.indexOf("j_security_check") > 0);
    }

    public static String processSSOCookie(CookieStore cookieStore, String serverA, String serverB) {
        String ssoID = null;
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("JSESSIONIDSSO".equalsIgnoreCase(cookie.getName())) {
                ssoID = cookie.getValue();
                if (serverA.equals(serverB) == false) {
                    // Make an sso cookie to send to serverB
                    Cookie copy = copyCookie(cookie, serverB);
                    cookieStore.addCookie(copy);
                }
            }
        }
        assertTrue("Didn't saw JSESSIONIDSSO", ssoID != null);
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

        // Cookie copy = new Cookie(targetServer, toCopy.getName(),
        // toCopy.getValue(), "/", null, false);
        return new BasicClientCookie(toCopy.getName(), toCopy.getValue());
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

}
