/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case where ejb subsytem option 'allow-ejb-name-regex' is not defined to true
 * and regex definition on ejb name for interceptor binding does nto work.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
public class RegexOptionNotDefinedTestCase {
    private static final String EJB_SIMPLE_RETURN = "test";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(RegexOptionNotDefinedTestCase.class.getPackage());
        war.addAsWebInfResource(RegexOptionNotDefinedTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testInterceptors() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assert.assertEquals(EJB_SIMPLE_RETURN, bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Test2");
        Assert.assertEquals(EJB_SIMPLE_RETURN, bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Production");
        Assert.assertEquals(EJB_SIMPLE_RETURN, bean.test());
    }
}
