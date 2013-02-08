/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.missingmethodpermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;

/**
 * Tests the <code>missing-method-permissions-deny-access</code> configuration which lets users decide whether secured beans whose
 * methods don't have explicit security configurations, are denied access or allowed access
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class MissingMethodPermissionsTestCase {

    private static final Logger logger = Logger.getLogger(MissingMethodPermissionsTestCase.class);

    private static final String APP_NAME = "missing-method-permissions-test-app";

    private static final String MODULE_ONE_NAME = "missing-method-permissions-test-ejb-jar-one";

    private static final String MODULE_TWO_NAME = "missing-method-permissions-test-ejb-jar-two";

    private LoginContext loginContext;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJarOne = ShrinkWrap.create(JavaArchive.class, MODULE_ONE_NAME + ".jar");
        ejbJarOne.addClasses(SecuredBeanOne.class);
        ejbJarOne.addAsManifestResource(SecuredBeanOne.class.getPackage(), "one-jboss-ejb3.xml", "jboss-ejb3.xml");

        final JavaArchive ejbJarTwo = ShrinkWrap.create(JavaArchive.class, MODULE_TWO_NAME + ".jar");
        ejbJarTwo.addClass(SecuredBeanTwo.class);
        ejbJarTwo.addAsManifestResource(SecuredBeanTwo.class.getPackage(), "two-jboss-ejb3.xml", "jboss-ejb3.xml");

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "bean-interfaces.jar");
        libJar.addClasses(SecurityTestRemoteView.class, Util.class, MissingMethodPermissionsTestCase.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsModules(ejbJarOne, ejbJarTwo);
        ear.addAsLibrary(libJar);

        return ear;
    }

    @Before
    public void login() throws Exception {
        final LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();

        this.loginContext = lc;
    }

    @After
    public void logout() throws Exception {
        if (this.loginContext != null) {
            this.loginContext.logout();
        }
        this.loginContext = null;
    }

    /**
     * Tests that methods without any explicit security permissions on an EJB marked
     * with <missing-method-permissions-deny-access>false</missing-method-permissions-deny-access> are allowed access
     *
     * @throws Exception
     */
    @Test
    public void testAllowAccessForMethodsMissingPermissions() throws Exception {
        final SecurityTestRemoteView allowAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_ONE_NAME + "/" + SecuredBeanOne.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
        // first invoke on a method which has a specific role and that invocation should pass
        final String callerPrincipalName = allowAccessBean.methodWithSpecificRole();
        Assert.assertEquals("Unexpected caller prinicpal", "user1", callerPrincipalName);
        // now invoke on a method which doesn't have an explicit security configuration. The SecuredBeanOne (deployment) is configured for
        // <missing-method-permissions-deny-access>false</missing-method-permissions-deny-access>
        // so the invocation on such a method is expected to fail
        final String callerPrincipalForMethodWithNoRole = allowAccessBean.methodWithNoRole();
        Assert.assertEquals("Unexpected caller prinicpal when invoking method with no role", "user1", callerPrincipalForMethodWithNoRole);
    }

    /**
     * Tests that methods without any explicit security permissions on an EJB marked
     * with <missing-method-permissions-deny-access>true</missing-method-permissions-deny-access> are denied access
     *
     * @throws Exception
     */
    @Test
    public void testDenyAccessForMethodsMissingPermissions() throws Exception {
        final SecurityTestRemoteView denyAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_TWO_NAME + "/" + SecuredBeanTwo.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
        // first invoke on a method which has a specific role and that invocation should pass
        final String callerPrincipalName = denyAccessBean.methodWithSpecificRole();
        Assert.assertEquals("Unexpected caller prinicpal", "user1", callerPrincipalName);
        // now invoke on a method which doesn't have an explicit security configuration. The SecuredBeanTwo (deployment) is configured for
        // <missing-method-permissions-deny-access>true</missing-method-permissions-deny-access>
        // so the invocation on such a method is expected to fail
        try {
            denyAccessBean.methodWithNoRole();
            Assert.fail("Invocation on a method with no specific security configurations was expected to fail due to <missing-method-permissions-deny-access>true</missing-method-permissions-deny-access> configuration, but it didn't");
        } catch (EJBAccessException eae) {
            logger.info("Got the expected exception", eae);
        }
    }
}
