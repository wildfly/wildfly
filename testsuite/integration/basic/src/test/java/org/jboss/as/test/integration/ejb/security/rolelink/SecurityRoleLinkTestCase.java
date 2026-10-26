/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.rolelink;

import java.io.File;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the security role linking via the ejb-jar.xml and the jboss-ejb3.xml works as expected
 *
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(SecurityRoleLinkTestCase.SecurityRoleLinkTestCaseSetup.class)
@Tag("CommonCriteria")
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
            Assertions.assertTrue(callerInRealRole, "Caller was expected to be in " + realRoleName + " but wasn't");

            final String aliasRoleName = "AliasRole";
            final boolean callerInAliasRole = callerRoleCheckerBean.isCallerInRole(aliasRoleName);
            Assertions.assertTrue(callerInAliasRole, "Caller was expected to be in " + aliasRoleName + " but wasn't");

            final String invalidRole = "UselessRole";
            final boolean callerInUselessRole = callerRoleCheckerBean.isCallerInRole(invalidRole);
            Assertions.assertFalse(callerInUselessRole, "Caller wasn't expected to be in " + invalidRole + " but was");
            return null;
        };
        try {
            Util.switchIdentity("phantom", "pass", callable);
        } catch (Exception e) {
            Assertions.fail(e.toString());
            throw e;
        }
    }
}
