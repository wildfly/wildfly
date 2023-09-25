/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case where a bean name matches two regex of interceptor bindings.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(RegexServerSetup.class)
public class MultiBeanMatchesRegexTestCase {
    private static final String EJB_INTERCEPTED = TestEjb.MESSAGE + RegexInterceptor.MESSAGE;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(MultiBeanMatchesRegexTestCase.class.getPackage());
        war.addAsWebInfResource(MultiBeanMatchesRegexTestCase.class.getPackage(), "ejb-jar-multi.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testInterceptors() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assert.assertEquals(EJB_INTERCEPTED, bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Test2");
        Assert.assertEquals(EJB_INTERCEPTED + "-regex", bean.test());
    }
}
