/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.regex;

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
 * Verifies that when allow-ejb-name-regex=false (the default), a pattern-like ejb-name
 * in jboss-ejb3.xml is treated as a literal bean name and does not match via regex.
 */
@RunWith(Arquillian.class)
public class EJBNameRegexDisabledContainerInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex-disabled.war");
        war.addPackage(EJBNameRegexDisabledContainerInterceptorTestCase.class.getPackage());
        war.addAsWebInfResource(EJBNameRegexDisabledContainerInterceptorTestCase.class.getPackage(), "jboss-ejb3-no-regex.xml", "jboss-ejb3.xml");
        return war;
    }

    @Test
    public void testPatternLikeEjbNameNotMatchedAsRegex() throws NamingException {
        TestInterceptorRegex.invoked = false;
        final InitialContext ctx = new InitialContext();
        SLSBRemote bean = (SLSBRemote) ctx.lookup("java:module/TestRemote");
        bean.foo();
        Assert.assertFalse("Interceptor must not be applied when regex is disabled and ejb-name is a pattern-like literal",
                TestInterceptorRegex.invoked);
    }
}
