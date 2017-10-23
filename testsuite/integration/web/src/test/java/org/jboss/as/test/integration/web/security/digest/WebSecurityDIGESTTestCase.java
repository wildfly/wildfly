/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.digest;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test case for web DIGEST authentication.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@ServerSetup({WebSecurityDigestSecurityDomainSetup.class})
@RunAsClient
public class WebSecurityDIGESTTestCase extends WebSecurityPasswordBasedBase {

    private static final String DEPLOYMENT = "digestApp";

    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "      xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun" +
            ".com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
            "      version=\"3.0\">\n" +
            "\n" +
            "\t<login-config>\n" +
            "\t\t<auth-method>DIGEST</auth-method>\n" +
            "\t\t<realm-name>" + WebSecurityDigestSecurityDomainSetup.SECURITY_DOMAIN_NAME + "</realm-name>\n" +
            "\t</login-config>\n" +
            "</web-app>";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(SimpleServlet.class, SimpleSecuredServlet.class);
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(WebSecurityDigestSecurityDomainSetup.SECURITY_DOMAIN_NAME),
                "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    /**
     * Check whether user with incorrect credentials has not access to secured page.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testWrongUser(@ArquillianResource URL url) throws Exception {
        makeCall(WebSecurityDigestSecurityDomainSetup.GOOD_USER_NAME, WebSecurityDigestSecurityDomainSetup
                .GOOD_USER_PASSWORD + "makeThisPasswordWrong", HTTP_UNAUTHORIZED);
    }

    /**
     * Check that after successful login, the nonce can be re-used without an extra 401 Unauthorized response loop.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testFollowupRequest(@ArquillianResource URL url) throws Exception {
        makeCallFollowup(WebSecurityDigestSecurityDomainSetup.GOOD_USER_NAME, WebSecurityDigestSecurityDomainSetup
                .GOOD_USER_PASSWORD, HTTP_OK, true);
    }

    @Override
    protected void makeCall(String user, String pass, int expectedCode) throws Exception {
        makeCallFollowup(user, pass, expectedCode, false);
    }

    protected void makeCallFollowup(String user, String pass, int expectedCode, boolean followup) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        Utils.makeCallWithBasicAuthn(servletUrl, user, pass, expectedCode, followup);
    }
}
