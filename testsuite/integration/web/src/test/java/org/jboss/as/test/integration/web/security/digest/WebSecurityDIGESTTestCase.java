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

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test case for web DIGEST authentication.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@ServerSetup({WebSecurityDIGESTTestCase.SecurityDomainsSetup.class})
@RunAsClient
public class WebSecurityDIGESTTestCase {

    private static final String SECURITY_DOMAIN_NAME = "digestSecurityDomain";
    private static final String DEPLOYMENT = "digestApp";

    private static final String GOOD_USER = "goodUser";
    private static final String GOOD_USER_PASSWORD = "Password1";
    private static final String USER_WITHOUT_NEEDED_ROLE = "user";
    private static final String USER_WITHOUT_NEEDED_ROLE_PASSWORD = "Password2";
    private static final String WRONG_USER = "wrongUser";
    private static final String WRONG_USER_PASSWORD = "Password13";
    private static final String GOOD_ROLE = SimpleSecuredServlet.ALLOWED_ROLE;
    private static final String WRONG_ROLE = "User";
    private static final String USERS = GOOD_USER + "=" + GOOD_USER_PASSWORD + "\n"
            + USER_WITHOUT_NEEDED_ROLE + "=" + USER_WITHOUT_NEEDED_ROLE_PASSWORD;
    private static final String ROLES = GOOD_USER + "=" + GOOD_ROLE + "\n" + USER_WITHOUT_NEEDED_ROLE + "=" + WRONG_ROLE;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(SimpleServlet.class, SimpleSecuredServlet.class);
        war.addAsResource(new StringAsset(USERS), "users.properties");
        war.addAsResource(new StringAsset(ROLES), "roles.properties");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_NAME), "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityDIGESTTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    /**
     * Check whether user with correct credentials has access to secured page.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testCorrectUser(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        String responseBody = Utils.makeCallWithBasicAuthn(servletUrl, GOOD_USER, GOOD_USER_PASSWORD, HTTP_OK);
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Check whether user with incorrect credentials has not access to secured page.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testWrongUser(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        Utils.makeCallWithBasicAuthn(servletUrl, WRONG_USER, WRONG_USER_PASSWORD, HTTP_UNAUTHORIZED);
    }

    /**
     * Check whether user with correct credentials but without needed role has not access to secured page.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    public void testUserWithoutNeededRole(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        Utils.makeCallWithBasicAuthn(servletUrl, USER_WITHOUT_NEEDED_ROLE, USER_WITHOUT_NEEDED_ROLE_PASSWORD, HTTP_FORBIDDEN);
    }

    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            final Map<String, String> lmOptions = new HashMap<>();
            lmOptions.put("hashAlgorithm", "MD5");
            lmOptions.put("hashEncoding", "RFC2617");
            lmOptions.put("hashUserPassword", "false");
            lmOptions.put("hashStorePassword", "true");
            lmOptions.put("passwordIsA1Hash", "false");
            lmOptions.put("storeDigestCallback", "org.jboss.security.auth.callback.RFC2617Digest");

            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .loginModules(new SecurityModule.Builder()
                            .name("UsersRoles")
                            .options(lmOptions)
                            .build())
                    .build();

            return new SecurityDomain[]{sd1};
        }

    }
}
