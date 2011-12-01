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

package org.jboss.as.test.integration.ejb.security;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
public class EJBSecurityTestCase {
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
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-security-test.jar");
        jar.addPackage(AnnotatedSLSB.class.getPackage());
        jar.addAsManifestResource("ejb/security/ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    private static <T> T lookup(final Class<?> beanClass, final Class<T> viewClass) throws NamingException {
        if (ctx == null)
            ctx = new InitialContext(); // to circumvent an Arquillian issue
        return viewClass.cast(ctx.lookup("java:module/" + beanClass.getSimpleName() + "!" + viewClass.getName()));
    }

    @Test
    public void testDenyAllAnnotation() throws Exception {
        final Context ctx = new InitialContext();
        final Restriction restrictedBean = (Restriction) ctx.lookup("java:module/" + AnnotatedSLSB.class.getSimpleName() + "!" + Restriction.class.getName());
        try {
            restrictedBean.restrictedMethod();
            Assert.fail("Call to restrictedMethod() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }

        final FullAccess fullAccessBean = (FullAccess) ctx.lookup("java:module/" + AnnotatedSLSB.class.getSimpleName() + "!" + FullAccess.class.getName());
        fullAccessBean.doAnything();

        final AnnotatedSLSB annotatedBean = (AnnotatedSLSB) ctx.lookup("java:module/" + AnnotatedSLSB.class.getSimpleName() + "!" + AnnotatedSLSB.class.getName());
        try {
            annotatedBean.restrictedMethod();
            Assert.fail("Call to restrictedMethod() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        }
        // full access, should work
        annotatedBean.doAnything();
        try {
            annotatedBean.restrictedBaseClassMethod();
            Assert.fail("Call to restrictedBaseClassMethod() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        }

        // should be accessible, since the overridden method isn't annotated with @DenyAll
        annotatedBean.overriddenMethod();

        final FullyRestrictedBean fullyRestrictedBean = (FullyRestrictedBean) ctx.lookup("java:module/" + FullyRestrictedBean.class.getSimpleName() + "!" + FullyRestrictedBean.class.getName());
        try {
            fullyRestrictedBean.overriddenMethod();
            Assert.fail("Call to overriddenMethod() method was expected to fail");

        } catch (EJBAccessException ejae) {
            // expected
        }

    }

    @Test
    public void testEJB2() throws Exception {
        // AS7-2809: if it deploys we're good
        final HelloRemote bean = lookup(HelloBean.class, HelloHome.class).create();
        final String result = bean.sayHello("EJB2");
        assertEquals("Hello EJB2", result);
    }

    @Test
    public void testExcludeList() throws Exception {
        final Context ctx = new InitialContext();
        final FullAccess fullAccessDDBean = (FullAccess) ctx.lookup("java:module/" + DDBasedSLSB.class.getSimpleName() + "!" + FullAccess.class.getName());
        fullAccessDDBean.doAnything();

        final DDBasedSLSB ddBasedSLSB = (DDBasedSLSB) ctx.lookup("java:module/" + DDBasedSLSB.class.getSimpleName() + "!" + DDBasedSLSB.class.getName());
        try {
            ddBasedSLSB.accessDenied();
            Assert.fail("Call to accessDenied() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }
        try {
            ddBasedSLSB.onlyTestRoleCanAccess();
            Assert.fail("Call to onlyTestRoleCanAccess() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected since only TestRole can call that method
        }
    }
}
