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
import org.junit.Test;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
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

import static org.jboss.as.naming.util.NamingUtils.asReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John E. Bailey
 */
public class NamingContextTestCase {

    private NamingContext namingContext;

    @Before
    public void setup() throws Exception {
        namingContext = new NamingContext(null);
    }

    @After
    public void cleanup() throws Exception {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testBindEmptyName() throws Exception {
        try {
            namingContext.bind(new CompositeName(), new Object());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            namingContext.bind(new CompositeName(""), new Object());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testBindInvalidContext() throws Exception {
        try {
            namingContext.bind(new CompositeName("bogus/test"), new Object());
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testBindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Object result = namingContext.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testBindAndLookupReference() throws Exception {
        final Name name = new CompositeName("test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blah", "test"), TestObjectFactory.class.getName(), null);

        namingContext.bind(name, reference);
        final Object result = namingContext.lookup(name);
        assertEquals("test", result);
    }

    @Test
    public void testBindAndLookupWithContinuation() throws Exception {
        namingContext.createSubcontext("comp");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "comp"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("test"), reference);

        namingContext.bind("test/nested", "test");

        final Object result = namingContext.lookup(new CompositeName("test/nested"));
        assertEquals("test", result);
    }

    @Test
    public void testBindAndLookupWitResolveResult() throws Exception {
        namingContext.createSubcontext("test");
        namingContext.bind("test/nested", "test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blahh", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("comp"), reference);

        final Object result = namingContext.lookup(new CompositeName("comp/nested"));
        assertEquals("test", result);
    }

    @Test
    public void testBindAndLookupLink() throws Exception {
        final Name name = new CompositeName("test");
        namingContext.bind(name, "testValue");
        final Name linkName = new CompositeName("link");
        namingContext.bind(linkName, new LinkRef("./test"));
        Object result = namingContext.lookup(linkName);
        assertEquals("testValue", result);

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        namingContext.rebind(linkName, new LinkRef(name));
        result = namingContext.lookup(linkName);
        assertEquals("testValue", result);
    }

    @Test
    public void testLookupNameNotFound() throws Exception {
        try {
            namingContext.lookup(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testLookupEmptyName() throws Exception {
        Object result = namingContext.lookup(new CompositeName());
        assertTrue(result instanceof NamingContext);
        result = namingContext.lookup(new CompositeName(""));
        assertTrue(result instanceof NamingContext);
    }

    @Test
    public void testUnbindNotFound() throws Exception {
        try {
            namingContext.unbind(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testBindUnbindLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Object result = namingContext.lookup(name);
        assertEquals(object, result);
        namingContext.unbind(name);
        try {
            namingContext.lookup(name);
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testUnbindAndLookupWithContinuation() throws Exception {
        namingContext.createSubcontext("comp");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "comp"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("test"), reference);

        final Name continuedName = new CompositeName("test/nested");
        namingContext.bind("test/nested", "test");
        assertEquals("test", namingContext.lookup(continuedName));
        namingContext.unbind("test/nested");
        try {
            namingContext.lookup("test/nested");
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testCreateSubcontextEmptyName() throws Exception {
        try {
            namingContext.createSubcontext(new CompositeName());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            namingContext.createSubcontext(new CompositeName(""));
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testCreateSubcontext() throws Exception {
        final Context context = namingContext.createSubcontext(new CompositeName("subcontext"));
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testCreateAndLookupSubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        namingContext.createSubcontext(name);
        final Context context = (Context)namingContext.lookup(name);
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testCreateSubcontextWithContinuation() throws Exception {
        namingContext.createSubcontext("test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("comp"), reference);

        final Context context = namingContext.createSubcontext(new CompositeName("comp/test"));
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testCreateAndDestroySubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        namingContext.createSubcontext(name);
        final Context context = (Context)namingContext.lookup(name);
        assertTrue(context instanceof NamingContext);

        namingContext.destroySubcontext(name);
        try {
            namingContext.lookup(name);
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testDestroyNonEmptySubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        final Context context = namingContext.createSubcontext(name);
        context.bind("test", new Object());
        try {
            namingContext.destroySubcontext(name);
            fail("Should have thrown and ContextNotEmptyException");
        } catch(ContextNotEmptyException expected){}
    }

    @Test
    public void testBindToSubcontext() throws Exception {
        final Name name = new CompositeName("subcontext");
        final Context context = namingContext.createSubcontext(name);
        final Object object = new Object();
        context.bind("test", object);
        Object result = context.lookup("test");
        assertEquals(object, result);

        result = namingContext.lookup(new CompositeName("subcontext/test"));
        assertEquals(object, result);
    }

    @Test
    public void testRebindEmptyName() throws Exception {
        try {
            namingContext.rebind(new CompositeName(), new Object());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}

        try {
            namingContext.rebind(new CompositeName(""), new Object());
            fail("Should have thrown and InvalidNameException");
        } catch(InvalidNameException expected){}
    }

    @Test
    public void testRebindInvalidContext() throws Exception {
        try {
            namingContext.rebind(new CompositeName("subcontext/test"), new Object());
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected){}
    }

    @Test
    public void testRebindAndLookup() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.rebind(name, object);
        final Object result = namingContext.lookup(name);
        assertEquals(object, result);
    }

    @Test
    public void testBindAndRebind() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.bind(name, object);
        assertEquals(object, namingContext.lookup(name));
        final Object objectTwo = new Object();
        namingContext.rebind(name, objectTwo);
        assertEquals(objectTwo, namingContext.lookup(name));
    }

    @Test
    public void testRebindAndLookupWithContinuation() throws Exception {
        namingContext.createSubcontext("comp");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "comp"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("test"), reference);

        final Name continuedName = new CompositeName("test/nested");
        namingContext.bind("test/nested", "test");
        assertEquals("test", namingContext.lookup(continuedName));
        namingContext.rebind("test/nested", "testTwo");
        assertEquals("testTwo", namingContext.lookup(continuedName));
    }

    @Test
    public void testListNameNotFound() throws Exception {
        try {
            namingContext.list(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testList() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        namingContext.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        namingContext.bind(nameThree, objectThree);

        namingContext.createSubcontext(new CompositeName("testContext"));

        final NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        while(results.hasMore()) {
            NameClassPair result = results.next();
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
    public void testListWithContinuation() throws Exception {
        namingContext.createSubcontext("test");

        final Name name = new CompositeName("test/test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Name nameTwo = new CompositeName("test/testTwo");
        final Object objectTwo = new Object();
        namingContext.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("test/testThree");
        final Object objectThree = new Object();
        namingContext.bind(nameThree, objectThree);

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("comp"), reference);

        final NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName("comp"));
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree"));
        while(results.hasMore()) {
            NameClassPair result = results.next();
            final String resultName = result.getName();
            if("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
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
        } catch(NameNotFoundException expected) {}
    }

    @Test
    public void testListBindings() throws Exception {
        final Name name = new CompositeName("test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Name nameTwo = new CompositeName("testTwo");
        final Object objectTwo = new Object();
        namingContext.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("testThree");
        final Object objectThree = new Object();
        namingContext.bind(nameThree, objectThree);

        namingContext.createSubcontext(new CompositeName("testContext"));

        final NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName());
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree", "testContext"));
        while(results.hasMore()) {
            final Binding result = results.next(); 
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
    public void testListBindingsWithContinuation() throws Exception {
        namingContext.createSubcontext("test");

        final Name name = new CompositeName("test/test");
        final Object object = new Object();
        namingContext.bind(name, object);
        final Name nameTwo = new CompositeName("test/testTwo");
        final Object objectTwo = new Object();
        namingContext.bind(nameTwo, objectTwo);
        final Name nameThree = new CompositeName("test/testThree");
        final Object objectThree = new Object();
        namingContext.bind(nameThree, objectThree);

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingContext.bind(new CompositeName("comp"), reference);

        final NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName("comp"));
        final Set<String> expected = new HashSet<String>(Arrays.asList("test", "testTwo", "testThree"));
        while(results.hasMore()) {
            NameClassPair result = results.next();
            final String resultName = result.getName();
            if("test".equals(resultName) || "testTwo".equals(resultName) || "testThree".equals(resultName)) {
                assertEquals(Object.class.getName(), result.getClassName());
            } else {
                fail("Unknown result name: " + resultName);
            }
            expected.remove(resultName);
        }
        assertTrue("Not all expected results were returned", expected.isEmpty());
    }

    public  static class TestObjectFactory implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return asReference(obj).get(0).getContent();
        }
    }

    public  static class TestObjectFactoryWithNameResolution implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            final Reference reference = asReference(obj);
            return new NamingContext(new CompositeName((String)reference.get(0).getContent()), null);
        }
    }
}