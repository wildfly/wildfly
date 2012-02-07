/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.cookie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for cookie
 *
 * @author prabhat.jha@jboss.com
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CookieUnitTestCase {

    protected static Logger log = Logger.getLogger(CookieUnitTestCase.class);

    protected static String[] cookieNames = { "simpleCookie", "withSpace", "commented", "expired" };

    protected static final long fiveSeconds = 5000;

    @ArquillianResource(CookieServlet.class)
    protected URL cookieURL;

    @ArquillianResource(CookieReadServlet.class)
    protected URL cookieReadURL;
    
    @Deployment(testable = false)
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-cookie.war");
        war.addClass(CookieReadServlet.class);
        war.addClass(CookieServlet.class);

        System.out.println(war.toString(true));
        return war;
    }

    @Test
    public void testCookieSetCorrectly() throws Exception {
        log.info("testCookieSetCorrectly()");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(cookieReadURL.toURI() + "CookieReadServlet"));
        if (response.getEntity() != null)
            response.getEntity().getContent().close();

        log.info("Sending request with cookie");
        response = httpclient.execute(new HttpPost(cookieReadURL.toURI() + "CookieReadServlet"));
    }

    @Test
    public void testCookieRetrievedCorrectly() throws Exception {
        log.info("testCookieRetrievedCorrectly()");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(cookieURL.toURI() + "CookieServlet"));

        // assert that we are able to hit servlet successfully
        int postStatusCode = response.getStatusLine().getStatusCode();
        Header[] postErrorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + postStatusCode, postStatusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(postErrorHeaders) + ") is null", postErrorHeaders.length == 0);

        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        assertTrue("Sever did not set expired cookie on client", checkNoExpiredCookie(cookies));

        for (Cookie cookie : cookies) {
            log.info("Cookie : " + cookie);
            String cookieName = cookie.getName();
            String cookieValue = cookie.getValue();

            if (cookieName.equals("simpleCookie")) {
                assertTrue("cookie value should be jboss", cookieValue.equals("jboss"));
                assertEquals("cookie path", "/jbosstest-cookie", cookie.getPath());
                assertEquals("cookie persistence", false, cookie.isPersistent());
            } else if (cookieName.equals("withSpace")) {
                assertEquals("should be no quote in cookie with space", cookieValue.indexOf("\""), -1);
            } else if (cookieName.equals("comment")) {
                log.info("comment in cookie: " + cookie.getComment());
                // RFC2109:Note that there is no Comment attribute in the Cookie request header
                // corresponding to the one in the Set-Cookie response header. The user
                // agent does not return the comment information to the origin server.

                assertTrue(cookie.getComment() == null);
            } else if (cookieName.equals("withComma")) {
                assertTrue("should contain a comma", cookieValue.indexOf(",") != -1);
            } else if (cookieName.equals("expireIn10Sec")) {
                Date now = new Date();
                log.info("will sleep for 5 seconds to see if cookie expires");
                assertTrue("cookies should not be expired by now", !cookie.isExpired(new Date(now.getTime() + fiveSeconds)));
                log.info("will sleep for 5 more secs and it should expire");
                assertTrue("cookies should be expired by now", cookie.isExpired(new Date(now.getTime() + 2 * fiveSeconds)));
            }
        }
    }

    protected boolean checkNoExpiredCookie(List<Cookie> cookies) {
        for (Cookie cookie : cookies)
            if (cookie.getName().equals("expired"))
                return false;
        return true;
    }
}
