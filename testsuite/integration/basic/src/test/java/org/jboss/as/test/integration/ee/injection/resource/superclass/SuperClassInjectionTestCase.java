/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.resource.superclass;

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
 * Tests that @Resource bindings on interceptors that are applied to multiple
 * components without their own naming context work properly, and do not try
 * and create two duplicate bindings in the same namespace.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SuperClassInjectionTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multiple-bindings-superclass.war");
        war.addClasses(Bean1.class, Bean2.class, SuperClassInjectionTestCase.class, SuperBean.class, SimpleManagedBean.class);
        war.addAsWebInfResource(SuperClassInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testCorrectBinding() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/" + SuperBean.class.getName() + "/simpleManagedBean");
        Assert.assertTrue(result instanceof SimpleManagedBean);
    }


    @Test
    public void testSubClass1Injected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean1 result = (Bean1) context.lookup("java:module/bean1");
        Assert.assertTrue(result.getBean() instanceof SimpleManagedBean);
    }

    @Test
    public void testSubClass2Injected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean2 result = (Bean2) context.lookup("java:module/bean2");
        Assert.assertTrue(result.getBean() instanceof SimpleManagedBean);
    }

    //AS7-6500
    @Test
    public void testOverridenInjectionIsNotInjected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean2 result = (Bean2) context.lookup("java:module/bean2");
        Assert.assertEquals("string2", result.getSimpleString());
        Assert.assertEquals(1, result.getSetCount());

    }

    @Test
    public void testNoInjectionOnOverride() throws NamingException {
        InitialContext context = new InitialContext();
        Bean1 result = (Bean1) context.lookup("java:module/bean1");
        Assert.assertNull(result.getSimpleString());
    }
}
