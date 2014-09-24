/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
@Category(CommonCriteria.class)
@ServerSetup({EjbSecurityDomainSetup.class})
public class EJBInWarDefaultSecurityDomainTestCase {
    private static Context ctx;

    @AfterClass
    public static void afterClass() throws NamingException {
        if (ctx != null)
            ctx.close();
    }

    @BeforeClass
    public static void beforeClass() throws NamingException {
        ctx = new InitialContext();
    }

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb-security-test.war");
        war.addClasses(BeanWithoutExplicitSecurityDomain.class, Restriction.class, FullAccess.class, EjbSecurityDomainSetup.class, Util.class);
        war.addAsWebInfResource(EJBInWarDefaultSecurityDomainTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addPackage(CommonCriteria.class.getPackage());
        war.addPackage(AbstractSecurityDomainSetup.class.getPackage());
        war.addAsResource(AnnotationAuthorizationTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(AnnotationAuthorizationTestCase.class.getPackage(), "roles.properties", "roles.properties");
        return war;
    }

    private static <T> T lookup(final Class<?> beanClass, final Class<T> viewClass) throws NamingException {
        if (ctx == null)
            ctx = new InitialContext(); // to circumvent an Arquillian issue
        return viewClass.cast(ctx.lookup("java:module/" + beanClass.getSimpleName() + "!" + viewClass.getName()));
    }

    /**
     * Tests that a bean which doesn't explicitly have a security domain configured, but still has EJB security related
     * annotations on it, is still considered secured and the security annotations are honoured
     *
     * @throws Exception
     */
    @Test
    public void testSecurityOnBeanInAbsenceOfExplicitSecurityDomain() throws Exception {
        final Context ctx = new InitialContext();
        // lookup the bean which doesn't explicitly have any security domain configured
        final Restriction restrictedBean = (Restriction) ctx.lookup("java:module/" + BeanWithoutExplicitSecurityDomain.class.getSimpleName() + "!" + Restriction.class.getName());
        try {
            // try invoking a method annotated @DenyAll (expected to fail)
            restrictedBean.restrictedMethod();
            Assert.fail("Call to restrictedMethod() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }

        // lookup the bean which doesn't explicitly have any security domain configured
        final FullAccess fullAccessBean = (FullAccess) ctx.lookup("java:module/" + BeanWithoutExplicitSecurityDomain.class.getSimpleName() + "!" + FullAccess.class.getName());
        // invoke a @PermitAll method
        fullAccessBean.doAnything();

        // lookup the bean which doesn't explicitly have any security domain configured
        final BeanWithoutExplicitSecurityDomain specificRoleAccessBean = (BeanWithoutExplicitSecurityDomain) ctx.lookup("java:module/" + BeanWithoutExplicitSecurityDomain.class.getSimpleName() + "!" + BeanWithoutExplicitSecurityDomain.class.getName());
        try {
            // invoke a method which only a specific role can access.
            // this is expected to fail since we haven't logged in as any user
            specificRoleAccessBean.allowOnlyRoleTwoToAccess();
            Assert.fail("Invocation was expected to fail since only a specific role was expected to be allowed to access the bean method");
        } catch (EJBAccessException ejbae) {
            // expected
        }


        // login as user1 and test
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            // expected to pass since user1 belongs to Role1
            specificRoleAccessBean.allowOnlyRoleOneToAccess();

            // expected to fail since user1 *doesn't* belong to Role2
            try {
                specificRoleAccessBean.allowOnlyRoleTwoToAccess();
                Assert.fail("Call to toBeInvokedByRole2() was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
        } finally {
            lc.logout();
        }

        // login as user2 and test
        lc = Util.getCLMLoginContext("user2", "password2");
        lc.login();
        try {
            // expected to pass since user2 belongs to Role2
            specificRoleAccessBean.allowOnlyRoleTwoToAccess();

            // expected to fail since user2 *doesn't* belong to Role1
            try {
                specificRoleAccessBean.allowOnlyRoleOneToAccess();
                Assert.fail("Call to toBeInvokedOnlyByRole1() was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
        } finally {
            lc.logout();
        }


    }
}
