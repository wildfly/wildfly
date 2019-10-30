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

package org.jboss.as.test.integration.ejb.security.singleton;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;

import javax.ejb.EJBAccessException;
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
                new ElytronPermission("getSecurityDomain")
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
