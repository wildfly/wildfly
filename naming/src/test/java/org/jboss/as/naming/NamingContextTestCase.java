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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John E. Bailey
 */
public class NamingContextTestCase {

    private NamingStore namingStore;
    private NamingContext namingContext;

    @BeforeClass
    public static void setupObjectFactoryBuilder() throws Exception {
        NamingContext.initializeNamingManager();
    }

    @Before
    public void setup() throws Exception {
        namingStore = new InMemoryNamingStore();
        NamingContext.setActiveNamingStore(namingStore);
        namingContext = new NamingContext(namingStore, null);
    }

    @After
    public void cleanup() throws Exception {
        namingStore.close();
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingStore.bind(name, object);

        final Object result = namingContext.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testLookupReference() throws Exception {
        final Name name = new CompositeName("test");
        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blah", "test"), TestObjectFactory.class.getName(), null);
        namingStore.bind(name, reference);

        final Object result = namingContext.lookup(name);
        assertEquals("test", result);
    }

    @Test
    public void testLookupWithContinuation() throws Exception {
        namingStore.bind(new CompositeName("comp/nested"), "test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "comp"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("test"), reference);

        final Object result = namingContext.lookup(new CompositeName("test/nested"));
        assertEquals("test", result);
    }

    @Test
    public void testLookupWitResolveResult() throws Exception {
        namingStore.bind(new CompositeName("test/nested"), "test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blahh", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("comp"), reference);

        final Object result = namingContext.lookup(new CompositeName("comp/nested"));
        assertEquals("test", result);
    }

    @Test
    public void testLookupLink() throws Exception {
        final Name name = new CompositeName("test");
        namingStore.bind(name, "testValue", String.class);
        final Name linkName = new CompositeName("link");
        namingStore.bind(linkName, new LinkRef("./test"));
        Object result = namingContext.lookup(linkName);
        assertEquals("testValue", result);

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        namingStore.rebind(linkName, new LinkRef(name));
        result = namingContext.lookup(linkName);
        assertEquals("testValue", result);
    }

    @Test
    public void testLookupContextLink() throws Exception {
        final Name name = new CompositeName("test/value");
        namingStore.bind(name, "testValue");
        final Name linkName = new CompositeName("link");
        namingStore.bind(linkName, new LinkRef("./test"));
        Object result = namingContext.lookup("link/value");
        assertEquals("testValue", result);
    }


    @Test
    public void testLookupNameNotFound() throws Exception {
        try {
            namingContext.lookup(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    public void testLookupEmptyName() throws Exception {
        Object result = namingContext.lookup(new CompositeName());
        assertTrue(result instanceof NamingContext);
        result = namingContext.lookup(new CompositeName(""));
        assertTrue(result instanceof NamingContext);
    }

    @Test
    public void testbind() throws Exception {
        try {
            namingContext.bind(new CompositeName("test"), new Object());
            fail("Should have thrown and UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testUnbind() throws Exception {
        try {
            namingContext.unbind(new CompositeName("test"));
            fail("Should have thrown and UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testCreateSubcontext() throws Exception {
        try {
            namingContext.createSubcontext(new CompositeName());
            fail("Should have thrown and UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testDestroyNonEmptySubcontext() throws Exception {
        try {
            namingContext.destroySubcontext(new CompositeName("subcontext"));
            fail("Should have thrown and UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }


    @Test
    public void testRebind() throws Exception {
        try {
            namingContext.rebind(new CompositeName(), new Object());
            fail("Should have thrown and UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testListNameNotFound() throws Exception {
        try {
            namingContext.list(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    public void testList() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingStore.bind(name, object);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        namingStore.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        namingStore.bind(nameThree, objectThree);

        namingStore.bind(new CompositeName("testContext/test"), "testNested");

        final NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        while (results.hasMore()) {
            NameClassPair result = results.next();
            final String resultName = result.getName();
            if ("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
            } else if ("testContext".equals(resultName)) {
                assertEquals(Context.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    @Test
    public void testListWithContinuation() throws Exception {
        final Name name = new CompositeName("test/test");
        final Object object = new Object();
        namingStore.bind(name, object);
        final Name nameTwo = new CompositeName("test/testTwo");
        final Object objectTwo = new Object();
        namingStore.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("test/testThree");
        final Object objectThree = new Object();
        namingStore.bind(nameThree, objectThree);

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("comp"), reference);

        final NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName("comp"));
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree"));
        while (results.hasMore()) {
            NameClassPair result = results.next();
            final String resultName = result.getName();
            if ("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
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
            namingContext.listBindings(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    public void testListBindings() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingStore.bind(name, object);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        namingStore.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        namingStore.bind(nameThree, objectThree);

        namingStore.bind(new CompositeName("testContext/test"), "test");

        final NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        while (results.hasMore()) {
            final Binding result = results.next();
            final String resultName = result.getName();
            if ("test".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(object, result.getObject());
            } else if ("testTwo".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(objectTwo, result.getObject());
            } else if ("testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
                assertEquals(objectThree, result.getObject());
            } else if ("testContext".equals(resultName)) {
                assertEquals(Context.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    @Test
    public void testListBindingsWithContinuation() throws Exception {
        final Name name = new CompositeName("test/test");
        final Object object = new Object();
        namingStore.bind(name, object);
        final Name nameTwo = new CompositeName("test/testTwo");
        final Object objectTwo = new Object();
        namingStore.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("test/testThree");
        final Object objectThree = new Object();
        namingStore.bind(nameThree, objectThree);

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("comp"), reference);

        final NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName("comp"));
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree"));
        while (results.hasMore()) {
            NameClassPair result = results.next();
            final String resultName = result.getName();
            if ("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    public static class TestObjectFactory implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return ((Reference) obj).get(0).getContent();
        }
    }

    public static class TestObjectFactoryWithNameResolution implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            final Reference reference = (Reference) obj;
            return new NamingContext(new CompositeName((String) reference.get(0).getContent()), null);
        }
    }
}
