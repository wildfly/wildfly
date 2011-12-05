/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.rolelink;

import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.security.SecurityTest;
import org.jboss.as.test.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the security role linking via the ejb-jar.xml and the jboss-ejb3.xml works as expected
 * 
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class SecurityRoleLinkTestCase {

    private static final String MODULE_NAME = "security-role-link-test";

    @Deployment
    public static Archive createDeployment() throws Exception {
        // setup the security-domain
        SecurityTest.createSecurityDomain(CallerRoleCheckerBean.SECURITY_DOMAIN_NAME);

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(CallerRoleCheckerBean.class.getPackage());
        jar.addClasses(Util.class, SecurityTest.class);
        jar.addAsResource("ejb/security/rolelink/users.properties", "users.properties");
        jar.addAsResource("ejb/security/rolelink/roles.properties", "roles.properties");
        jar.addAsManifestResource("ejb/security/rolelink/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/security/rolelink/jboss-ejb3.xml", "jboss-ejb3.xml");

        return jar;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SecurityTest.removeSecurityDomain(CallerRoleCheckerBean.SECURITY_DOMAIN_NAME);
    }

    /**
     * Test that when the security role linking via the security-role-ref element in the ejb-jar.xml
     * takes into account the security-role mapping between the principal and the role name in the jboss-ejb3.xml.
     * 
     * @throws Exception
     */
    @Test
    public void testIsCallerInRole() throws Exception {
        final CallerRoleCheckerBean callerRoleCheckerBean = InitialContext.doLookup("java:module/" + CallerRoleCheckerBean.class.getSimpleName());

        final LoginContext loginContext = Util.getCLMLoginContext("phantom", "pass");
        loginContext.login();
        try {
            final String realRoleName = "RealRole";
            final boolean callerInRealRole = callerRoleCheckerBean.isCallerInRole(realRoleName);
            Assert.assertTrue("Caller was expected to be in " + realRoleName + " but wasn't", callerInRealRole);

            final String aliasRoleName = "AliasRole";
            final boolean callerInAliasRole = callerRoleCheckerBean.isCallerInRole(aliasRoleName);
            Assert.assertTrue("Caller was expected to be in " + aliasRoleName + " but wasn't", callerInAliasRole);

            final String invalidRole = "UselessRole";
            final boolean callerInUselessRole = callerRoleCheckerBean.isCallerInRole(invalidRole);
            Assert.assertFalse("Caller wasn't expected to be in " + invalidRole + " but was", callerInUselessRole);

        } finally {
            loginContext.logout();
        }
    }
}
