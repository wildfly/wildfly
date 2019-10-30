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

import static org.junit.Assert.assertEquals;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
@Category(CommonCriteria.class)
public class EJBSecurityTestCase {

    @ArquillianResource
    private Context ctx;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-security-test.jar");
        jar.addPackage(AnnotatedSLSB.class.getPackage());
        jar.addAsManifestResource(EJBSecurityTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(EJBSecurityTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    private <T> T lookup(final Class<?> beanClass, final Class<T> viewClass) throws NamingException {
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

    }

    /**
     * Tests that if a method of an EJB is annotated with a {@link javax.annotation.security.RolesAllowed} with empty value for the annotation
     * <code>@RolesAllowed({})</code> then access to that method by any user MUST throw an EJBAccessException. i.e. it should
     * behave like a @DenyAll
     *
     * @throws Exception
     */
    @Test
    public void testEmptyRolesAllowedAnnotationValue() throws Exception {
        final Context ctx = new InitialContext();

        final AnnotatedSLSB annotatedBean = (AnnotatedSLSB) ctx.lookup("java:module/" + AnnotatedSLSB.class.getSimpleName() + "!" + AnnotatedSLSB.class.getName());
        try {
            annotatedBean.methodWithEmptyRolesAllowedAnnotation();
            Assert.fail("Call to methodWithEmptyRolesAllowedAnnotation() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        }
    }
}
