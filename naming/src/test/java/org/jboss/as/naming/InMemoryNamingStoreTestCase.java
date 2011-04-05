/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.spi.ResolveResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John E. Bailey
 */
public class InMemoryNamingStoreTestCase {

    private final InMemoryNamingStore nameStore = new InMemoryNamingStore();

    @After
    public void cleanup() throws Exception {
        nameStore.close();
    }

    @Test
    public void testBindEmptyName() throws Exception {
        try {
            nameStore.bind(new CompositeName(), new Object(), Object.class);
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            nameStore.bind(new CompositeName(""), new Object(), Object.class);
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testBindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(name, object, Object.class);
        final Object result = nameStore.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testLookupNameNotFound() throws Exception {
        try {
            nameStore.lookup(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testLookupEmptyName() throws Exception {
        Object result = nameStore.lookup(new CompositeName());
        assertTrue(result instanceof NamingContext);
        result = nameStore.lookup(new CompositeName(""));
        assertTrue(result instanceof NamingContext);
    }

    @Test
    public void testBindAndLookupResolveResult() throws Exception {
        final Name name = new CompositeName("test");
        final Reference reference = new Reference(Context.class.getName());
        nameStore.bind(name, reference, Context.class);
        final Object result = nameStore.lookup(new CompositeName("test/value"));
        assertTrue(result instanceof ResolveResult);
    }

    @Test
    public void testUnbindNotFound() throws Exception {
        try {
            nameStore.unbind(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testBindUnbindLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(name, object, Object.class);
        final Object result = nameStore.lookup(name);
        assertEquals(object, result);
        nameStore.unbind(name);
        try {
            nameStore.lookup(name);
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testRebindEmptyName() throws Exception {
        try {
            nameStore.rebind(new CompositeName(), new Object(), Object.class);
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            nameStore.rebind(new CompositeName(""), new Object(), Object.class);
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testRebindInvalidContext() throws Exception {
        try {
            nameStore.rebind(new CompositeName("subcontext/test"), new Object(), Object.class);
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testRebindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.rebind(name, object, Object.class);
        final Object result = nameStore.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testBindAndRebind() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(name, object, Object.class);
        assertEquals(object, nameStore.lookup(name));
        final Object objectTwo = new Object();
        nameStore.rebind(name, objectTwo, Object.class);
        assertEquals(objectTwo, nameStore.lookup(name));
    }

    @Test
    public void testListNameNotFound() throws Exception {
        try {
            nameStore.list(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testList() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(name, object, Object.class);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        nameStore.bind(nameTwo, objectTwo, Object.class);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        nameStore.bind(nameThree, objectThree, Object.class);

        nameStore.bind(new CompositeName("testContext/test"), "test");

        final List<NameClassPair> results = nameStore.list(new CompositeName());
        assertEquals(4, results.size());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        for(NameClassPair result : results) {
            final String resultName = result.getName();
            if("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
            } else if("testContext".equals(resultName)) {
                assertEquals(Context.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    @Test
    public void testListBindingsNameNotFound() throws Exception {
        try {
            nameStore.listBindings(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testListBindings() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(name, object);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        nameStore.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        nameStore.bind(nameThree, objectThree);

        nameStore.bind(new CompositeName("testContext/test"), "test");

        final List<Binding> results = nameStore.listBindings(new CompositeName());
        assertEquals(4, results.size());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        for(Binding result : results) {
            final String resultName = result.getName();
            if("test".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(object, result.getObject());
            } else if("testTwo".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(objectTwo, result.getObject());
            } else if("testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(objectThree, result.getObject());
            } else if("testContext".equals(resultName)) {
                assertEquals(Context.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    @Test
    public void testAutoRemove() throws Exception {
        nameStore.bind(new CompositeName("test/item"), new Object());

        assertNotNull(nameStore.lookup(new CompositeName("test/item")));
        assertNotNull(nameStore.lookup(new CompositeName("test")));

        nameStore.unbind(new CompositeName("test/item"));

        try {
            nameStore.lookup(new CompositeName("test"));
            fail("Should have throw name not found exception");
        } catch (NameNotFoundException expected){}
    }
}
