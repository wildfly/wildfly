/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.method;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class EjbMethodInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testmethodinterceptor.war");
        war.addPackage(EjbMethodInterceptorTestCase.class.getPackage());
        war.addAsWebInfResource(EjbMethodInterceptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testMethodLevelInterceptors() throws NamingException {
        final InitialContext ctx = new InitialContext();
        final ClassifiedBean bean = (ClassifiedBean) ctx.lookup("java:module/" + ClassifiedBean.class.getSimpleName());

        TopSecretInterceptor.called = false;
        SecretInterceptor.called = false;
        final String secret = bean.secretMethod();
        Assertions.assertEquals("Secret", secret);
        Assertions.assertTrue(SecretInterceptor.called);
        Assertions.assertFalse(TopSecretInterceptor.called);

        Assertions.assertTrue(SecretInterceptor.postConstructCalled);
        Assertions.assertFalse(TopSecretInterceptor.postConstructCalled);

        String topSecret = bean.topSecretMethod();
        Assertions.assertEquals("TopSecret", topSecret);
        Assertions.assertTrue(TopSecretInterceptor.called);
        Assertions.assertFalse(TopSecretInterceptor.postConstructCalled);
    }

    @Test
    public void testMethodOverloaded() throws NamingException {
        final InitialContext ctx = new InitialContext();
        final ClassifiedBean bean = (ClassifiedBean) ctx.lookup("java:module/" + ClassifiedBean.class.getSimpleName());

        TopSecretInterceptor.called = false;
        SecretInterceptor.called = false;
        final String ret1 = bean.overloadedMethod(1);
        Assertions.assertEquals("ArgInt:1", ret1);
        Assertions.assertTrue(SecretInterceptor.called);
        Assertions.assertFalse(TopSecretInterceptor.called);

        TopSecretInterceptor.called = false;
        SecretInterceptor.called = false;
        final String ret2 = bean.overloadedMethod("1");
        Assertions.assertEquals("ArgStr:1", ret2);
        Assertions.assertTrue(SecretInterceptor.called);
        Assertions.assertTrue(TopSecretInterceptor.called);
    }

    @Test
    public void testAroundInvokeOverridedByXmlDescriptor() throws NamingException {
        InitialContext ctx = new InitialContext();
        AroundInvokeBean bean = (AroundInvokeBean) ctx.lookup("java:module/" + AroundInvokeBean.class.getSimpleName());
        final String message = bean.call();
        Assertions.assertEquals("InterceptedDD:Hi", message);
    }
}
