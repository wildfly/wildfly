/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
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
 * Checking if default interceptor binding works if ejb3 subsystem attribute
 * 'allow-ejb-name-regex' is set to true.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(RegexServerSetup.class)
public class DefaultInterceptorRegexTestCase {
    private static final String EJB_RETURN = TestEjb.MESSAGE;
    private static final String EJB_INTERCEPTED = TestEjb.MESSAGE + RegexInterceptor.MESSAGE;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(DefaultInterceptorRegexTestCase.class.getPackage());
        war.addAsWebInfResource(DefaultInterceptorRegexTestCase.class.getPackage(), "ejb-jar-default.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void defaultInterceptor() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assert.assertEquals(EJB_INTERCEPTED, bean.test());
    }

    @Test
    public void defaultInterceptorAnnotatedBean() throws NamingException {
        final InitialContext ctx = new InitialContext();
        AnnotatedEjb bean = (AnnotatedEjb) ctx.lookup("java:module/" + AnnotatedEjb.class.getName());
        Assert.assertEquals(EJB_INTERCEPTED, bean.test());
    }

    @Test
    public void annotationExcludeInterceptor() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assert.assertEquals(EJB_RETURN, bean.testIgnoreDefault());
    }
}
