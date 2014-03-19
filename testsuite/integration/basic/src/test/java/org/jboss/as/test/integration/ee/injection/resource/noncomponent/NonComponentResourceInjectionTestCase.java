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
package org.jboss.as.test.integration.ee.injection.resource.noncomponent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that @Resource annotations on classes that are not managed by the container are ignored.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class NonComponentResourceInjectionTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "non-component.war");
        war.addPackage(NonComponentResourceInjectionTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testBindingDoesNotExist() throws NamingException {
        try {
            InitialContext context = new InitialContext();
            Object result = context.lookup("java:module/env/" + NonComponentResourceInjectionTestCase.class.getName() + "/userTransaction");
            Assert.fail();
        } catch (NamingException expected) {

        }
    }


    @Test
    public void testBindingExists() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/" + ComponentResourceInjection.class.getName() + "/userTransaction");
        Assert.assertNotNull(result);
    }

}
