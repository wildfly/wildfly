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
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask;
import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Arquillian JUnit testcase for testing XACML based authorization of EJBs.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ EjbXACMLAuthorizationModuleTestCase.CustomXACMLAuthzSecurityDomainSetup.class })
public class EjbXACMLAuthorizationModuleTestCase {
    private static Logger LOGGER = Logger.getLogger(EjbXACMLAuthorizationModuleTestCase.class);

    @EJB(mappedName = "java:global/test-custom-xacml/HelloBean")
    private Hello hello;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link JavaArchive} for testing the {@link CustomXACMLAuthorizationModule}.
     * 
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Deployment
    public static JavaArchive deploymentCustomXACMLAuthz() throws IllegalArgumentException, IOException {
        return createJar("test-custom-xacml.jar", CustomXACMLAuthzSecurityDomainSetup.SECURITY_DOMAIN_NAME);
    }

    /**
     * Tests secured EJB call for unauthenticated user.
     * 
     * @throws Exception
     */
    @Test
    public void testNotAuthn() throws Exception {
        try {
            hello.sayHello();
            fail("Access to sayHello() should be denied if not authenticated.");
        } catch (EJBAccessException e) {
            //OK
        }
    }

    /**
     * Test secured EJB call for authenticated and authorized user.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthz() throws Exception {
        SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("jduke", "theduke");
        try {
            securityClient.login();
            assertEquals(HelloBean.HELLO_WORLD, hello.sayHello());
        } finally {
            securityClient.logout();
        }
    }

    /**
     * Test secured EJB call for authenticated but not authorized authorized user.
     * 
     * @throws Exception
     */
    @Test
    @Ignore("JBPAPP-8989")
    public void testNotAuthz() throws Exception {
        SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("JohnDoe", "jdoe");
        try {
            securityClient.login();
            hello.sayHello();
            fail("Access to sayHello() should be denied for JohnDoe.");
        } catch (EJBAccessException e) {
            //OK - expected
        } finally {
            securityClient.logout();
        }
    }

    /**
     * Tests unauthenticated call followed by the authentication and second call to the same instance.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticationCache() throws Exception {
        try {
            hello.sayHello();
            fail("Access to sayHello() should be denied if not authenticated.");
        } catch (EJBAccessException e) {
            //OK
        }
        SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("jduke", "theduke");
        try {
            securityClient.login();
            assertEquals(HelloBean.HELLO_WORLD, hello.sayHello());
        } finally {
            securityClient.logout();
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates JAR with the EJB for the test deployment.
     * 
     * @param archiveName
     * @param securityDomainName
     * @return
     */
    private static JavaArchive createJar(final String archiveName, final String securityDomainName) {
        final JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, archiveName)
                .addClasses(HelloBean.class, Hello.class, CustomXACMLAuthorizationModule.class)
                .addAsResource(new StringAsset("jduke=theduke\nJohnDoe=jdoe"), "users.properties")
                .addAsResource(new StringAsset("jduke=TestRole,TestRole2\nJohnDoe=TestRole"), "roles.properties")
                .addAsResource(EjbXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_CONFIG + "/jbossxacml-config.xml", "jbossxacml-config.xml")
                .addAsManifestResource(EjbXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_POLICIES + "/ejb-xacml-policy.xml", "xacml-policy.xml")
                .addAsManifestResource(EjbXACMLAuthorizationModuleTestCase.class.getPackage(),
                        XACMLTestUtils.TESTOBJECTS_CONFIG + "/jboss-ejb3.xml", "jboss-ejb3.xml");
        XACMLTestUtils.addJBossDeploymentStructureToArchive(jar);
        jar.addClasses(AbstractSecurityDomainStackServerSetupTask.class);
        LOGGER.info(jar.toString(true));
        return jar;
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

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getAuthorizationModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getAuthorizationModuleConfigurations() {
            return createModuleConfiguration(CustomXACMLAuthorizationModule.class.getName());
        }

    }

}
