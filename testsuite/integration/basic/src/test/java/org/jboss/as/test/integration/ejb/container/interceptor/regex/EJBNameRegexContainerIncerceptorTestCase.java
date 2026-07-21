/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.regex;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.interceptor.regex.RegexServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@RunWith(Arquillian.class)
@ServerSetup(RegexServerSetup.class)
public class EJBNameRegexContainerIncerceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(EJBNameRegexContainerIncerceptorTestCase.class.getPackage());
        war.addAsWebInfResource(EJBNameRegexContainerIncerceptorTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return war;
    }

    @Test
    public void test() throws NamingException {
        resetInterceptors();
        final InitialContext ctx = new InitialContext();
        SLSBRemote bean = (SLSBRemote) ctx.lookup("java:module/TestRemote");
        bean.foo();
        Assert.assertTrue(TestInterceptorFullName.invoked);
        Assert.assertTrue(TestInterceptorRegex.invoked);
        Assert.assertFalse(TestInterceptorWrongRegex.invoked);
    }

    private void resetInterceptors() {
        TestInterceptorFullName.invoked = false;
        TestInterceptorRegex.invoked = false;
        TestInterceptorWrongRegex.invoked = false;
    }
}
