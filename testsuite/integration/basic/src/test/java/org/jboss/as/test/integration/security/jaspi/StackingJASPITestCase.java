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

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
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

/**
 * This test-case validates JASPI authn-modules stacking.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ StackingJASPITestCase.SecurityDomainsSetup.class })
@RunAsClient
public class StackingJASPITestCase {

    private static Logger LOGGER = Logger.getLogger(StackingJASPITestCase.class);

    private final static String SECURITY_DOMAIN1 = "jaspi-test-1";
    private final static String SECURITY_DOMAIN2 = "jaspi-test-2";
    private final static String SECURITY_DOMAIN3 = "jaspi-test-3";

    // Public methods --------------------------------------------------------

    /**
     * Creates a testing WebApp for {@link SecurityDomain1Setup}.
     * 
     * @return
     */
    @Deployment(name = SECURITY_DOMAIN1)
    public static WebArchive deployment1() {
        return createWar(SECURITY_DOMAIN1);
    }

    /**
     * Creates a testing WebApp for {@link SecurityDomain2Setup}.
     * 
     * @return
     */
    @Deployment(name = SECURITY_DOMAIN2)
    public static WebArchive deployment2() {
        return createWar(SECURITY_DOMAIN2);
    }

    /**
     * Creates a testing WebApp for {@link SecurityDomain3Setup}.
     * 
     * @return
     */
    @Deployment(name = SECURITY_DOMAIN3)
    public static WebArchive deployment3() {
        return createWar(SECURITY_DOMAIN3);
    }

    /**
     * Tests 2 "required" authn-modules - the both passes.
     * 
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SECURITY_DOMAIN1)
    public void testDomain1(@ArquillianResource URL webAppURL) throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing - " + webAppUrl);
        final String responseBody = Utils.makeCallWithBasicAuthn(webAppUrl, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Expected response body doesn't match the returned one.", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Tests 2 "required" authn-modules - the second fails.
     * 
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SECURITY_DOMAIN2)
    public void testDomain2(@ArquillianResource URL webAppURL) throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing - " + webAppUrl);
        Utils.makeCallWithBasicAuthn(webAppUrl, "jduke", "theduke", HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Tests "sufficient" flag in authn-module.
     * 
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SECURITY_DOMAIN3)
    public void testDomain3(@ArquillianResource URL webAppURL) throws Exception {
        final URL webAppUrl = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.info("Testing - " + webAppUrl);
        final String responseBody = Utils.makeCallWithBasicAuthn(webAppUrl, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Expected response body doesn't match the returned one.", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates WAR which has configured given security domain and it's name is the same name as the domain name (with ".war"
     * suffix).
     * 
     * @param securityDomainName
     * @return a new WebArchive
     */
    private static WebArchive createWar(final String securityDomainName) {
        final String archiveName = securityDomainName + ".war";
        LOGGER.info("Creating " + archiveName);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, archiveName);
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class, FailureAuthModule.class, SuccessAuthModule.class);
        war.addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + // 
                "<security-domain>" + securityDomainName + "</security-domain>" + //
                "<valve><class-name>" + WebJASPIAuthenticator.class.getName() + "</class-name></valve>" + //
                "</jboss-web>"), "jboss-web.xml");
        war.addAsResource(new StringAsset("jduke=theduke\ntester=password"), "users.properties");
        war.addAsResource(new StringAsset("jduke=Authenticated,JBossAdmin\ntester=Authenticated"), "roles.properties");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(war.toString(true));
        }
        return war;
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
            final LoginModuleStack lmStack = new LoginModuleStack.Builder().name("lmStack")
                    .loginModules(new SecurityModule.Builder().name("UsersRoles").build()).build();
            final AuthnModule failureAuthnModule = new AuthnModule.Builder().name(FailureAuthModule.class.getName())
                    .flag(Constants.REQUIRED).build();

            final AuthnModule.Builder authnModuleBuilder = new AuthnModule.Builder()
                    .name(HTTPBasicServerAuthModule.class.getName()).flag(Constants.REQUIRED).loginModuleStackRef("lmStack");

            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN1)
                    .jaspiAuthn(
                            new JaspiAuthn.Builder()
                                    .loginModuleStacks(lmStack)
                                    .authnModules(
                                            authnModuleBuilder.build(), //
                                            new AuthnModule.Builder().name(SuccessAuthModule.class.getName())
                                                    .flag(Constants.REQUIRED).build()) //
                                    .build()) //
                    .build();
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN2)
                    .jaspiAuthn(
                            new JaspiAuthn.Builder().loginModuleStacks(lmStack).authnModules(authnModuleBuilder.build(), //
                                    failureAuthnModule) //
                                    .build()) //
                    .build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN3)
                    .jaspiAuthn(
                            new JaspiAuthn.Builder().loginModuleStacks(lmStack)
                                    .authnModules(authnModuleBuilder.flag(Constants.SUFFICIENT).build(), //
                                            failureAuthnModule) //
                                    .build()) //
                    .build();

            return new SecurityDomain[] { sd1, sd2, sd3 };
        }
    }

}
