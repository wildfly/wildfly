/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Interceptor invocation context testing.
 *
 * @author Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
public class InvocationContextTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "invocation-context.jar");
        jar.addPackage(InvocationContextTestCase.class.getPackage());
        jar.addAsManifestResource(InvocationContextTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testInvocationContext() throws NamingException {
        final InvocationBean bean = (InvocationBean) ctx.lookup("java:module/" + InvocationBean.class.getSimpleName());

        final String result = bean.callMethod(1, "invoked");
        Assertions.assertEquals("DefaultOK:ClassOK:MethodOK:BeanOK:invokedDefaultClassMethodBean", result);
    }

    @Test
    public void tesTimerInvocationContext() throws NamingException {
        TimeoutBean bean = (TimeoutBean) ctx.lookup("java:module/" + TimeoutBean.class.getSimpleName());
        bean.createTimer();
        Assertions.assertTrue(TimeoutBean.awaitTimerCall());
        Assertions.assertEquals("TimeoutDefaultOK:TimeoutClassOK:TimeoutMethodOK:TimeoutBeanOK:@Timeout", TimeoutBean.interceptorResults);
    }
}
