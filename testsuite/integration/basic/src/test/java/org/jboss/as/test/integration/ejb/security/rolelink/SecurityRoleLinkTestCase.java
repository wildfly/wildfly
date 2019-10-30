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

import java.io.File;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests that the security role linking via the ejb-jar.xml and the jboss-ejb3.xml works as expected
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(SecurityRoleLinkTestCase.SecurityRoleLinkTestCaseSetup.class)
@Category(CommonCriteria.class)
public class SecurityRoleLinkTestCase {

    private static final String MODULE_NAME = "security-role-link-test";

    static class SecurityRoleLinkTestCaseSetup extends EjbSecurityDomainSetup {
        @Override
        protected String getSecurityDomainName() {
            return CallerRoleCheckerBean.SECURITY_DOMAIN_NAME;
        }

        @Override
        protected String getUsersFile() {
            return new File(SecurityRoleLinkTestCase.class.getResource("users.properties").getFile()).getAbsolutePath();
        }

        @Override
        protected String getGroupsFile() {
            return new File(SecurityRoleLinkTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath();
        }
    }

    @Deployment
    public static Archive createDeployment() throws Exception {
        final Package currentPackage = SecurityRoleLinkTestCase.class.getPackage();

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(CallerRoleCheckerBean.class.getPackage())
                .addClasses(Util.class, SecurityRoleLinkTestCaseSetup.class)
                .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class)
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage,"roles.properties", "roles.properties")
                .addAsManifestResource(currentPackage,"ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(currentPackage,"jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(currentPackage, "permissions.xml", "permissions.xml")
                .addPackage(CommonCriteria.class.getPackage());

        return jar;
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

        final Callable<Void> callable = () -> {
            final String realRoleName = "RealRole";
            final boolean callerInRealRole = callerRoleCheckerBean.isCallerInRole(realRoleName);
            Assert.assertTrue("Caller was expected to be in " + realRoleName + " but wasn't", callerInRealRole);

            final String aliasRoleName = "AliasRole";
            final boolean callerInAliasRole = callerRoleCheckerBean.isCallerInRole(aliasRoleName);
            Assert.assertTrue("Caller was expected to be in " + aliasRoleName + " but wasn't", callerInAliasRole);

            final String invalidRole = "UselessRole";
            final boolean callerInUselessRole = callerRoleCheckerBean.isCallerInRole(invalidRole);
            Assert.assertFalse("Caller wasn't expected to be in " + invalidRole + " but was", callerInUselessRole);
            return null;
        };
        try {
            Util.switchIdentity("phantom", "pass", callable);
        } catch (Exception e) {
            Assert.fail(e.toString());
            throw e;
        }
    }
}
