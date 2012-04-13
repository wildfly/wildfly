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
package org.jboss.as.test.integration.security.xacml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Arquillian testcase, which tests access to web applications protected by XACML authorization modules.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ WebXACMLAuthorizationModuleTestCase.XACMLAuthzSecurityDomainSetup.class,
        WebXACMLAuthorizationModuleTestCase.CustomXACMLAuthzSecurityDomainSetup.class })
@RunAsClient
public class WebXACMLAuthorizationModuleTestCase {

    private static final Logger LOGGER = Logger.getLogger(WebXACMLAuthorizationModuleTestCase.class);

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} for the XACMLAuthorizationModule testing.
     * 
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Deployment(name = "XACML")
    public static WebArchive deploymentDefaultXACML() throws IllegalArgumentException, IOException {
        return createWar(XACMLAuthzSecurityDomainSetup.SECURITY_DOMAIN_NAME, "xacml-web-test.war");
    }

    /**
     * Creates {@link WebArchive} for the {@link CustomXACMLAuthorizationModule} testing.
     * 
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Deployment(name = "CustomXACML")
    public static WebArchive deploymentCustomXACML() throws IllegalArgumentException, IOException {
        return createWar(CustomXACMLAuthzSecurityDomainSetup.SECURITY_DOMAIN_NAME, "custom-xacml-web-test.war");
    }

    /**
     * Test access to the web application protected by XACMLAuthorizationModule.
     * 
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @Ignore("JBPAPP-8773")
    @OperateOnDeployment("XACML")
    public void testWebUsingXACMLAuthz(@ArquillianResource URL webAppURL) throws Exception {
        testWebAccess(webAppURL);
    }

    /**
     * Test access to the web application protected by {@link CustomXACMLAuthorizationModule}.
     * 
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("CustomXACML")
    public void testWebUsingCustomXACMLAuthz(@ArquillianResource URL webAppURL) throws Exception {
        testWebAccess(webAppURL);
    }

    // Private methods -------------------------------------------------------

    /**
     * Make requests to protected resource and asserts the response code is the expected one.
     * 
     * @param webAppURL
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void testWebAccess(URL webAppURL) throws MalformedURLException, ClientProtocolException, IOException,
            URISyntaxException {
        final URL requestURL = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        final String response = Utils.makeCallWithBasicAuthn(requestURL, "jduke", "theduke", 200);
        assertEquals(SimpleSecuredServlet.RESPONSE_BODY, response);
        Utils.makeCallWithBasicAuthn(requestURL, "guest", "guest", 403);
    }

    /**
     * Creates web application with given name and with the given security domain name configured.
     * 
     * @param domainName
     * @param warName
     * @return
     */
    private static WebArchive createWar(String domainName, String warName) {
        final WebArchive war = ShrinkWrap
                .create(WebArchive.class, warName)
                .addClasses(SimpleSecuredServlet.class, CustomXACMLAuthorizationModule.class)
                .addAsWebInfResource(
                        new StringAsset("<jboss-web><security-domain>" + domainName
                                + "</security-domain><use-jboss-authorization>true</use-jboss-authorization></jboss-web>"),
                        "jboss-web.xml")
                .addAsWebInfResource(WebXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_CONFIG + "/web-basic-authn.xml", "web.xml")
                .addAsResource(new StringAsset("jduke=theduke\nguest=guest"), "users.properties")
                .addAsResource(new StringAsset("jduke=AuthorizedUser,ServletUserRole\nguest=AuthorizedUser"),
                        "roles.properties")
                .addAsResource(WebXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_CONFIG + "/jbossxacml-config.xml", "jbossxacml-config.xml")
                .addAsResource(WebXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_POLICIES + "/web-xacml-policy.xml", "xacml-policy.xml");
        XACMLTestUtils.addJBossDeploymentStructureToArchive(war);
        LOGGER.info(war.toString(true));
        return war;
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A security domain ServerSetupTask for XACML tests - creates a domain with UsersRoles LoginModule and XACML policy module.
     */
    static class XACMLAuthzSecurityDomainSetup extends AbstractSecurityDomainStackServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getAuthorizationModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getAuthorizationModuleConfigurations() {
            return createModuleConfiguration("XACML");
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            return createModuleConfiguration("UsersRoles");
        }

        /**
         * Creates a simple single SecurityModuleConfiguration instance with the given module name. Returns it in an array of
         * size 1.
         * 
         * @param moduleName
         * @return single-element
         */
        protected final SecurityModuleConfiguration[] createModuleConfiguration(final String moduleName) {
            final SecurityModuleConfiguration loginModule = new AbstractSecurityModuleConfiguration() {
                public String getName() {
                    return moduleName;
                }
            };
            return new SecurityModuleConfiguration[] { loginModule };
        }
    }

    /**
     * A security domain ServerSetupTask for XACML tests, which uses the {@link CustomXACMLAuthorizationModule} as the
     * authorization/policy module.
     */
    static class CustomXACMLAuthzSecurityDomainSetup extends XACMLAuthzSecurityDomainSetup {

        public static final String SECURITY_DOMAIN_NAME = "custom-xacml-authz-security-domain";

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getAuthorizationModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getAuthorizationModuleConfigurations() {
            return createModuleConfiguration(CustomXACMLAuthorizationModule.class.getName());
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }
    }

}
