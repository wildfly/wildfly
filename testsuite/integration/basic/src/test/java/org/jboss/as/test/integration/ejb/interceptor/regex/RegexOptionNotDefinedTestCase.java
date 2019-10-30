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
