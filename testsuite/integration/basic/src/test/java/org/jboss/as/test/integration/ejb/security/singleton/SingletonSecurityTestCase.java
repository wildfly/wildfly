/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.singleton;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;

import jakarta.ejb.EJBAccessException;
import javax.naming.InitialContext;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Tests that invocations on a secured singleton bean work as expected.
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@Category(CommonCriteria.class)
public class SingletonSecurityTestCase {
    private static final Logger log = Logger.getLogger(SingletonSecurityTestCase.class.getName());

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-security.jar");
        jar.addPackage(SingletonSecurityTestCase.class.getPackage());
        jar.addPackage(CommonCriteria.class.getPackage());
        jar.addClass(Util.class);
        jar.addAsResource(createPermissionsXmlAsset(
                new RuntimePermission("org.jboss.security.setSecurityContext"),
                new ElytronPermission("getSecurityDomain"),
                new ElytronPermission("authenticate"),
                new ElytronPermission("setRunAsPrincipal")
                ), "META-INF/permissions.xml");
        return jar;
    }

    /**
     * Test a method invocation on a singleton bean with the correct expected role.
     *
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        final Callable<Void> callable = () -> {
            // expects role1, so should succeed
            securedSingleton.allowedForRole1();
            return null;
        };
        Util.switchIdentitySCF("user1", "password1", callable);

    }

    /**
     * Test a method invocation on a singleton bean with an incorrect role.
     *
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithInCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        final Callable<Void> callable = () -> {
            // expects role1, so should fail
            securedSingleton.allowedForRole1();
            Assert.fail("Call to secured method, with incorrect role, was expected to fail");
            return null;
        };
        try {
            Util.switchIdentitySCF("user2", "password2", callable);
        } catch (EJBAccessException ejbae) {
            // expected
        }

    }

    /**
     * Test a method invocation on a singleton bean without logging in.
     *
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithoutLogin() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        try {
            securedSingleton.allowedForRole1();
            Assert.fail("Call to secured method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }

    }
}
