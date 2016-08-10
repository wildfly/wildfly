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
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
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
@ServerSetup({WebXACMLAuthorizationModuleTestCase.SecurityDomainsSetup.class})
@RunAsClient
public class WebXACMLAuthorizationModuleTestCase {

    private static final Logger LOGGER = Logger.getLogger(WebXACMLAuthorizationModuleTestCase.class);

    private static final String SECURITY_DOMAIN_XACML = "XACML";
    private static final String SECURITY_DOMAIN_CUSTOM = "CustomXACML";

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
        return createWar(SECURITY_DOMAIN_XACML, "xacml-web-test.war");
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
        return createWar(SECURITY_DOMAIN_CUSTOM, "custom-xacml-web-test.war");
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
    private void testWebAccess(URL webAppURL) throws IOException,
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
            final SecurityModule loginModule = new SecurityModule.Builder().name("UsersRoles").build();
            final SecurityDomain sd1 = new SecurityDomain.Builder().name(SECURITY_DOMAIN_XACML).loginModules(loginModule)
                    .authorizationModules(new SecurityModule.Builder().name("XACML").build()) //
                    .build();
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_CUSTOM)
                    .loginModules(loginModule)
                    .authorizationModules(
                            new SecurityModule.Builder().name(CustomXACMLAuthorizationModule.class.getName()).build()) //
                    .build();
            return new SecurityDomain[]{sd1, sd2};
        }
    }
}
