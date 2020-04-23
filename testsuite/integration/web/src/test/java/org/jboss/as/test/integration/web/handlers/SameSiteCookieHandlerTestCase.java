/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.handlers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import io.undertow.server.handlers.CookieSameSiteMode;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests the use of SameSiteCookieHandler
 *
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SameSiteCookieHandlerTestCase {

    // war file names
    private static final String PLAIN_WEB_APP = "cookie-servlet.war";
    private static final String LAX_SAMESITE_COOKIE_WEB_APP = "lax-samesite-cookie-servlet.war";
    private static final String STRICT_SAMESITE_COOKIE_WEB_APP = "strict-samesite-cookie-servlet.war";
    private static final String NONE_SAMESITE_COOKIE_WEB_APP = "none-samesite-cookie-servlet.war";
    private static final String NONE_SAMESITE_UNSECURE_COOKIE_WEB_APP = "none-samesite-unsecure-cookie-servlet.war";

    // handler config
    private static final String SAMESITE_COOKIE_HANDLER_NAME = "samesite-cookie";
    private static final String SAMESITE_COOKIE_LAX_HANDLER = SAMESITE_COOKIE_HANDLER_NAME
            + "(" + CookieSameSiteMode.LAX + ")";
    private static final String SAMESITE_COOKIE_STRICT_HANDLER = SAMESITE_COOKIE_HANDLER_NAME
            + "(" + CookieSameSiteMode.STRICT + ")";
    private static final String SAMESITE_COOKIE_NONE_HANDLER = SAMESITE_COOKIE_HANDLER_NAME
            + "(" + CookieSameSiteMode.NONE + ")";
    private static final String SAMESITE_COOKIE_NONE_HANDLER_UNSECURE = SAMESITE_COOKIE_HANDLER_NAME
            + "(mode=" + CookieSameSiteMode.NONE + ",add-secure-for-none=false)";

    @WebServlet(name = "CookieServlet", urlPatterns = {"/cookieservlet"})
    public static class CookieServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws
                ServletException, IOException {
            PrintWriter out = resp.getWriter();
            resp.addCookie(new Cookie("cookie", "created-by-servlet"));
            out.print("hello world");
            out.close();
        }
    }

    @Deployment(name = PLAIN_WEB_APP)
    public static WebArchive deploy_plain_war() {
        return ShrinkWrap.create(WebArchive.class, PLAIN_WEB_APP)
                .addClass(CookieServlet.class);
    }

    @Deployment(name = LAX_SAMESITE_COOKIE_WEB_APP)
    public static WebArchive deploy_war_with_samesite_lax() {
        return ShrinkWrap.create(WebArchive.class, LAX_SAMESITE_COOKIE_WEB_APP)
                .addClass(CookieServlet.class)
                .addAsWebInfResource(new StringAsset(
                        SAMESITE_COOKIE_LAX_HANDLER), "undertow-handlers.conf");
    }

    @Deployment(name = STRICT_SAMESITE_COOKIE_WEB_APP)
    public static WebArchive deploy_war_with_samesite_strict() {
        return ShrinkWrap.create(WebArchive.class,
                STRICT_SAMESITE_COOKIE_WEB_APP)
                .addClass(CookieServlet.class)
                .addAsWebInfResource(new StringAsset(
                        SAMESITE_COOKIE_STRICT_HANDLER), "undertow-handlers.conf");
    }

    @Deployment(name = NONE_SAMESITE_COOKIE_WEB_APP)
    public static WebArchive deploy_war_with_samesite_none() {
        return ShrinkWrap.create(WebArchive.class, NONE_SAMESITE_COOKIE_WEB_APP)
                .addClass(CookieServlet.class)
                .addAsWebInfResource(new StringAsset(
                        SAMESITE_COOKIE_NONE_HANDLER), "undertow-handlers.conf");
    }

    @Deployment(name = NONE_SAMESITE_UNSECURE_COOKIE_WEB_APP)
    public static WebArchive deploy_war_with_samesite_none_unsecure() {
        return ShrinkWrap.create(WebArchive.class,
                NONE_SAMESITE_UNSECURE_COOKIE_WEB_APP)
                .addClass(CookieServlet.class)
                .addAsWebInfResource(new StringAsset(
                        SAMESITE_COOKIE_NONE_HANDLER_UNSECURE), "undertow-handlers.conf");
    }

    @Test
    @OperateOnDeployment(PLAIN_WEB_APP)
    public void testWebAppWithoutHandler(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/cookieservlet"), null, false);
    }

    @Test
    @OperateOnDeployment(LAX_SAMESITE_COOKIE_WEB_APP)
    public void testWebAppWithLaxSameSiteCookieHandler(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/cookieservlet"), CookieSameSiteMode.LAX, false);
    }

    @Test
    @OperateOnDeployment(STRICT_SAMESITE_COOKIE_WEB_APP)
    public void testWebAppWithStrictSameSiteCookieHandler(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/cookieservlet"), CookieSameSiteMode.STRICT, false);
    }

    @Test
    @OperateOnDeployment(NONE_SAMESITE_COOKIE_WEB_APP)
    public void testWebAppWithNoneSameSiteCookieHandler(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/cookieservlet"), CookieSameSiteMode.NONE, true);
    }

    @Test
    @OperateOnDeployment(NONE_SAMESITE_UNSECURE_COOKIE_WEB_APP)
    public void testWebAppWithNoneSameSiteUnsecureCookieHandler(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/cookieservlet"), CookieSameSiteMode.NONE, false);
    }

    private void commonTestPart(URL url, CookieSameSiteMode mode, boolean secure) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpget = new HttpGet(url.toExternalForm());
            HttpResponse response = httpClient.execute(httpget);

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            Header[] hdrs = response.getHeaders("set-cookie");
            Assert.assertEquals(1, hdrs.length);
            if (secure) {
                String cookieValue = hdrs[0].getValue();
                Assert.assertEquals("cookie=created-by-servlet; secure; SameSite=" + mode, cookieValue);
            } else {
                String expectedCookie = "cookie=created-by-servlet" + (mode == null? "" :"; SameSite=" + mode);
                Assert.assertEquals(expectedCookie, hdrs[0].getValue());
            }
        }
    }
}
