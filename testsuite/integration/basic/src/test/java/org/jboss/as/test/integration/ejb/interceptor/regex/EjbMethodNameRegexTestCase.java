/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Testing of interceptor binding which binds ejb name based on regex.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(RegexServerSetup.class)
public class EjbMethodNameRegexTestCase {
    private static final String EJB_RETURN = TestEjb.MESSAGE;
    private static final String EJB_INTERCEPTED = TestEjb.MESSAGE + RegexInterceptor.MESSAGE;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(EjbMethodNameRegexTestCase.class.getPackage());
        war.addAsWebInfResource(EjbMethodNameRegexTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testInterceptors() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assertions.assertEquals(EJB_INTERCEPTED, bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Test2");
        Assertions.assertEquals(EJB_INTERCEPTED, bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Production");
        Assertions.assertEquals(EJB_RETURN, bean.test());
    }

    @Test
    public void annotatedBeanName() throws NamingException {
        final InitialContext ctx = new InitialContext();
        AnnotatedEjb bean = (AnnotatedEjb) ctx.lookup("java:module/" + AnnotatedEjb.class.getName());
        Assertions.assertEquals(EJB_INTERCEPTED, bean.test());
    }

    @Test
    public void classLevelExcludeInerceptor() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assertions.assertEquals(EJB_RETURN, bean.testIgnoreClass());
        TestEjb beanProduction = (TestEjb) ctx.lookup("java:module/Production");
        Assertions.assertEquals(EJB_RETURN, beanProduction.testIgnoreClass());

        TestEjb bean2 = (TestEjb) ctx.lookup("java:module/Test2");
        Assertions.assertEquals(EJB_INTERCEPTED, bean2.testIgnoreDefault());
        Assertions.assertEquals(EJB_RETURN, beanProduction.testIgnoreDefault());
    }

    @Test
    public void methodLevelInterceptor() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/.AnotherTest");
        Assertions.assertEquals(EJB_RETURN, bean.test());
        Assertions.assertEquals(EJB_INTERCEPTED, bean.testIgnoreClass());
        Assertions.assertEquals(EJB_RETURN, bean.testIgnoreDefault());

        TestEjb yetBean = (TestEjb) ctx.lookup("java:module/YetAnotherTest");
        Assertions.assertEquals(EJB_INTERCEPTED, yetBean.test());
    }

    @Test
    public void interceptorOrder() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/OrderingTest");
        Assertions.assertEquals(EJB_INTERCEPTED + "-regex", bean.test());
    }
}
