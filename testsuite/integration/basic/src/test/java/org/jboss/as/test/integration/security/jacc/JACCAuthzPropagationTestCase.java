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
package org.jboss.as.test.integration.security.jacc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.jacc.propagation.BridgeBean;
import org.jboss.as.test.integration.security.jacc.propagation.Manage;
import org.jboss.as.test.integration.security.jacc.propagation.PropagationTestServlet;
import org.jboss.as.test.integration.security.jacc.propagation.TargetBean;
import org.jboss.as.test.integration.security.loginmodules.UsersRolesLoginModuleTestCase;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests, which checks run-as identity handling in EJB JACC authorization module.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({JACCAuthzPropagationTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Ignore("See WFLY-4989")
public class JACCAuthzPropagationTestCase {

    private static final Logger LOGGER = Logger.getLogger(JACCAuthzPropagationTestCase.class);

    private static final String TEST_NAME = Manage.TEST_NAME;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} deployment.
     */
    @Deployment(name = "war")
    public static WebArchive warDeployment() {
        LOGGER.trace("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, TEST_NAME + ".war");
        war.addClasses(PropagationTestServlet.class, Manage.class, BridgeBean.class, TargetBean.class);
        final StringAsset usersRolesAsset = new StringAsset(Utils.createUsersFromRoles(Manage.ROLES_ALL));
        war.addAsResource(usersRolesAsset, "users.properties");
        war.addAsResource(usersRolesAsset, "roles.properties");

        war.addAsWebInfResource(UsersRolesLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(TEST_NAME), "jboss-web.xml");
        war.addAsWebInfResource(Utils.getJBossEjb3XmlAsset(TEST_NAME), "jboss-ejb3.xml");
        return war;

    }

    /**
     * Tests direct permissions (RolesAllowed).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    public void testTarget(@ArquillianResource URL webAppURL) throws Exception {
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_TARGET, PropagationTestServlet.METHOD_NAME_ADMIN, Manage.ROLE_ADMIN);
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_TARGET, PropagationTestServlet.METHOD_NAME_MANAGE, Manage.ROLE_MANAGER);
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_TARGET, PropagationTestServlet.METHOD_NAME_WORK, Manage.ROLE_ADMIN);

        assertAccessDenied(webAppURL, Manage.BEAN_NAME_TARGET, PropagationTestServlet.METHOD_NAME_ADMIN, Manage.ROLE_MANAGER);
    }

    /**
     * Tests run-as permissions.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    public void testBridge(@ArquillianResource URL webAppURL) throws Exception {
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_MANAGE, Manage.ROLE_ADMIN);
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_MANAGE, Manage.ROLE_MANAGER);
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_MANAGE, Manage.ROLE_USER);
        assertAccessAllowed(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_WORK, Manage.ROLE_USER);

        assertAccessDenied(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_ADMIN, Manage.ROLE_ADMIN);
        assertAccessDenied(webAppURL, Manage.BEAN_NAME_BRIDGE, PropagationTestServlet.METHOD_NAME_ADMIN, Manage.ROLE_MANAGER);
    }

    // Private methods -------------------------------------------------------

    /**
     * Asserts the access to the given method in the given bean is allowed for given role.
     *
     * @param webAppURL
     * @param beanName
     * @param methodName
     * @param roleName
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void assertAccessAllowed(URL webAppURL, String beanName, String methodName, String roleName)
            throws IOException, URISyntaxException {
        final URL testUrl = getTestURL(webAppURL, beanName, methodName);
        assertEquals("Access of role " + roleName + " to " + methodName + " method in " + beanName + " should be allowed.",
                Manage.RESULT, Utils.makeCallWithBasicAuthn(testUrl, roleName, roleName, 200));
    }

    /**
     * Asserts the access to the given method in the given bean is denied for given role.
     *
     * @param webAppURL
     * @param beanName
     * @param methodName
     * @param roleName
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void assertAccessDenied(URL webAppURL, String beanName, String methodName, String roleName)
            throws IOException, URISyntaxException {
        final URL testUrl = getTestURL(webAppURL, beanName, methodName);
        assertEquals("Access of role " + roleName + " to " + methodName + " method in " + beanName + " should be denied.",
                PropagationTestServlet.RESULT_EJB_ACCESS_EXCEPTION,
                Utils.makeCallWithBasicAuthn(testUrl, roleName, roleName, 200));
    }

    /**
     * Creates URL of the test application with the given values of request parameters.
     *
     * @param webAppURL
     * @param beanName
     * @param method
     * @return
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException
     */
    private URL getTestURL(URL webAppURL, String beanName, String method) throws MalformedURLException,
            UnsupportedEncodingException {
        return new URL(webAppURL.toExternalForm() + PropagationTestServlet.SERVLET_PATH.substring(1) + "?" //
                + PropagationTestServlet.PARAM_BEAN_NAME + "=" + beanName + "&" //
                + PropagationTestServlet.PARAM_METHOD_NAME + "=" + method);
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {
        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            return new SecurityDomain[]{new SecurityDomain.Builder().name(TEST_NAME)
                    .loginModules(new SecurityModule.Builder().name("UsersRoles").flag(Constants.REQUIRED).build()) //
                    .authorizationModules(new SecurityModule.Builder().name("JACC").flag(Constants.REQUIRED).build()) //
                    .cacheType("default") //
                    .build()};
        }
    }
}
