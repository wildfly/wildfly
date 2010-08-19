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
            nameStore.bind(null, new CompositeName(), new Object(), Object.class.getName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            nameStore.bind(null, new CompositeName(""), new Object(), Object.class.getName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testBindInvalidContext() throws Exception {
        try {
            nameStore.bind(null, new CompositeName("subcontext/test"), new Object(), Object.class.getName());
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testBindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(null, name, object, Object.class.getName());
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
        nameStore.bind(null, name, reference, Context.class.getName());
        final Object result = nameStore.lookup(new CompositeName("test/value"));
        assertTrue(result instanceof ResolveResult);
    }

    @Test
    public void testUnbindNotFound() throws Exception {
        try {
            nameStore.unbind(null, new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testBindUnbindLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(null, name, object, Object.class.getName());
        final Object result = nameStore.lookup(name);
        assertEquals(object, result);
        nameStore.unbind(null, name);
        try {
            nameStore.lookup(name);
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testCreateSubcontextEmptyName() throws Exception {
        try {
            nameStore.createSubcontext(null, new CompositeName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            nameStore.createSubcontext(null, new CompositeName(""));
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testCreateSubcontext() throws Exception {
        final Context context = nameStore.createSubcontext(null, new CompositeName("subcontext"));
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testCreateAndLookupSubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        nameStore.createSubcontext(null, name);
        final Context context = (Context)nameStore.lookup(name);
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testBindToSubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        final Context context = nameStore.createSubcontext(null, name);
        final Object object = new Object();
        context.bind("test", object);
        Object result = context.lookup("test");
        assertEquals(object, result);

        result = nameStore.lookup(new CompositeName("subcontext/test"));
        assertEquals(object, result);
    }

    @Test
    public void testRebindEmptyName() throws Exception {
        try {
            nameStore.rebind(null, new CompositeName(), new Object(), Object.class.getName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            nameStore.rebind(null, new CompositeName(""), new Object(), Object.class.getName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testRebindInvalidContext() throws Exception {
        try {
            nameStore.rebind(null, new CompositeName("subcontext/test"), new Object(), Object.class.getName());
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testRebindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.rebind(null, name, object, Object.class.getName());
        final Object result = nameStore.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testBindAndRebind() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        nameStore.bind(null, name, object, Object.class.getName());
        assertEquals(object, nameStore.lookup(name));
        final Object objectTwo = new Object();
        nameStore.rebind(null, name, objectTwo, Object.class.getName());
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
        nameStore.bind(null, name, object, Object.class.getName());
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        nameStore.bind(null, nameTwo, objectTwo, Object.class.getName());
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        nameStore.bind(null, nameThree, objectThree, Object.class.getName());

        nameStore.createSubcontext(null, new CompositeName("testContext"));

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
        nameStore.bind(null, name, object, Object.class.getName());
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        nameStore.bind(null, nameTwo, objectTwo, Object.class.getName());
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        nameStore.bind(null, nameThree, objectThree, Object.class.getName());

        nameStore.createSubcontext(null, new CompositeName("testContext"));

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
}
