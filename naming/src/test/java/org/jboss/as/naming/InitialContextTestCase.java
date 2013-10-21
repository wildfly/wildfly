/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.naming;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;


/**
 * @author David Bosschaert
 */
public class InitialContextTestCase {

    @Before
    public void before() {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testRegisterURLSchemeHandler() throws Exception {
        InitialContext ictx = new InitialContext(null);

        try {
            ictx.lookup("foobar:something");
            Assert.fail("Precondition: the foobar: scheme should not yet be registered");
        } catch (NamingException ne) {
            // good
        }

        ObjectFactory tof = new TestObjectFactory();
        InitialContext.addUrlContextFactory("foobar", tof);
        String something = (String) ictx.lookup("foobar:something");
        Assert.assertTrue("The object should now be provided by our TestObjectFactory", something.startsWith("TestObject:"));

        try {
            InitialContext.removeUrlContextFactory("foobar:", new TestObjectFactory());
            Assert.fail("Should throw an IllegalArgumentException since the associated factory object doesn't match the registration");
        } catch (IllegalArgumentException iae) {
            // good;
        }

        Assert.assertEquals("The foobar: scheme should still be registered", something, ictx.lookup("foobar:something"));

        InitialContext.removeUrlContextFactory("foobar", tof);
        try {
            ictx.lookup("foobar:something");
            Assert.fail("The foobar: scheme should not be registered any more");
        } catch (NamingException ne) {
            // good
        }
    }

    private class TestObjectFactory implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return new InitialContext(new Hashtable<String, Object>()) {
                @Override
                public Object lookup(Name name) throws NamingException {
                    return "TestObject: " + name;
                }
                @Override
                public Object lookup(String name) throws NamingException {
                    return "TestObject: " + name;
                }
            };
        }
    }
}
