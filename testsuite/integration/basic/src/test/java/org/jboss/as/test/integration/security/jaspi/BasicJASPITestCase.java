/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.jaspi;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.AuthnModule;
import org.jboss.as.test.integration.security.common.config.JaspiAuthn;
import org.jboss.as.test.integration.security.common.config.LoginModuleStack;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleServlet;
import org.jboss.as.web.security.jaspi.WebJASPIAuthenticator;
import org.jboss.as.web.security.jaspi.modules.HTTPBasicServerAuthModule;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup({ BasicJASPITestCase.SecurityDomainsSetup.class })
@RunAsClient
public class BasicJASPITestCase {

    private static Logger LOGGER = Logger.getLogger(BasicJASPITestCase.class);

    public static final String SECURITY_DOMAIN_NAME = "test-security-domain";

    @ArquillianResource
    URL webAppURL;

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} with the {@link OKServlet}.
     * 
     * @return
     * @throws SQLException
     */
    @Deployment
    public static WebArchive deployment() {
        LOGGER.info("start deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jaspi-test.war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class);
        war.addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + // 
                "<security-domain>" + SECURITY_DOMAIN_NAME + "</security-domain>" + //
                "<valve><class-name>" + WebJASPIAuthenticator.class.getName() + "</class-name></valve>" + //
                "</jboss-web>"), "jboss-web.xml");
        war.addAsResource(new StringAsset("jduke=theduke\ntester=password"), "users.properties");
        war.addAsResource(new StringAsset("jduke=Authenticated,JBossAdmin\ntester=Authenticated"), "roles.properties");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(war.toString(true));
        }
        return war;
    }

    /**
     * Correct login.
     * 
     * @throws Exception
     */
    @Test
    public void testSuccesfullAuth() throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing successfull authentication - " + webAppUrl);
        final String responseBody = Utils.makeCallWithBasicAuthn(webAppUrl, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Expected response body doesn't match the returned one.", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Incorrect login.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthn() throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing failed authentication - " + webAppUrl);
        Utils.makeCallWithBasicAuthn(webAppUrl, "anil", "theduke", HttpServletResponse.SC_UNAUTHORIZED);
        Utils.makeCallWithBasicAuthn(webAppUrl, "jduke", "anil", HttpServletResponse.SC_UNAUTHORIZED);
        Utils.makeCallWithBasicAuthn(webAppUrl, "anil", "anil", HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Correct login, but without permissions.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthz() throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing failed authorization - " + webAppUrl);
        Utils.makeCallWithBasicAuthn(webAppUrl, "tester", "password", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Unsecured request.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecured() throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing access to unprotected resource - " + webAppUrl);
        final String responseBody = Utils.makeCallWithBasicAuthn(webAppUrl, null, null, HttpServletResponse.SC_OK);
        assertEquals("Expected response body doesn't match the returned one.", SimpleServlet.RESPONSE_BODY, responseBody);
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     * 
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         * 
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .jaspiAuthn(
                            new JaspiAuthn.Builder()
                                    .loginModuleStacks(
                                            new LoginModuleStack.Builder().name("lmStack")
                                                    .loginModules(new SecurityModule.Builder().name("UsersRoles").build())
                                                    .build())
                                    .authnModules(
                                            new AuthnModule.Builder().name(HTTPBasicServerAuthModule.class.getName())
                                                    .flag(Constants.REQUIRED).loginModuleStackRef("lmStack").build()).build())
                    .build();
            return new SecurityDomain[] { sd1 };
        }
    }
}
