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

import static org.jboss.as.naming.SecurityHelper.testActionPermission;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.naming.java.permission.JndiPermission;

/**
 * @author John E. Bailey
 */
public class NamingContextTestCase {

    private WritableNamingStore namingStore;
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

        Object result = namingContext.lookup(name);
        assertEquals(object, result);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, "test");
        assertEquals(object, result);
    }

    @Test
    public void testLookupReference() throws Exception {
        final Name name = new CompositeName("test");
        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blah", "test"), TestObjectFactory.class.getName(), null);
        namingStore.bind(name, reference);

        Object result = namingContext.lookup(name);
        assertEquals("test", result);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, "test");
        assertEquals("test", result);
    }

    @Test
    public void testLookupWithContinuation() throws Exception {
        namingStore.bind(new CompositeName("comp/nested"), "test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("nns", "comp"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("test"), reference);

        Object result = namingContext.lookup(new CompositeName("test/nested"));
        assertEquals("test", result);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, Arrays.asList(new JndiPermission("comp/nested", "lookup")), namingContext, "test/nested");
        assertEquals("test", result);
    }

    @Test
    public void testLookupWitResolveResult() throws Exception {
        namingStore.bind(new CompositeName("test/nested"), "test");

        final Reference reference = new Reference(String.class.getName(), new StringRefAddr("blahh", "test"), TestObjectFactoryWithNameResolution.class.getName(), null);
        namingStore.bind(new CompositeName("comp"), reference);

        Object result = namingContext.lookup(new CompositeName("comp/nested"));
        assertEquals("test", result);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, Arrays.asList(new JndiPermission("test/nested", "lookup")), namingContext, "comp/nested");
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

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, Arrays.asList(new JndiPermission("test", "lookup")), namingContext, "link");
        assertEquals("testValue", result);

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        namingStore.rebind(linkName, new LinkRef(name));
        result = namingContext.lookup(linkName);
        assertEquals("testValue", result);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, Arrays.asList(new JndiPermission("test", "lookup")), namingContext, "link");
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

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, Arrays.asList(new JndiPermission("test", "lookup"),
                new JndiPermission("test/value", "lookup")), namingContext, "link/value");

        assertEquals("testValue", result);
    }


    @Test
    public void testLookupNameNotFound() throws Exception {
        try {
            namingContext.lookup(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        //the same with security permissions
        try {
            testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, "test");
            fail("Should have thrown and NameNotFoundException with appropriate permissions");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    public void testLookupEmptyName() throws Exception {
        Object result = namingContext.lookup(new CompositeName());
        assertTrue(result instanceof NamingContext);
        result = namingContext.lookup(new CompositeName(""));
        assertTrue(result instanceof NamingContext);

        //the same with security permissions
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, null);
        assertTrue(result instanceof NamingContext);
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, "");
        assertTrue(result instanceof NamingContext);
    }

    @Test
    public void testBind() throws Exception {
        Name name = new CompositeName("test");
        final Object value = new Object();
        namingContext.bind(name, value);
        assertEquals(value, namingStore.lookup(name));

        //the same with security permissions
        name = new CompositeName("securitytest");
        testActionPermission(JndiPermission.ACTION_BIND, namingContext, "securitytest", value);
        assertEquals(value, namingStore.lookup(name));
    }

    @Test
    public void testBindReferenceable() throws Exception {
        Name name = new CompositeName("test");
        final TestObjectReferenceable referenceable = new TestObjectReferenceable("addr");
        namingContext.bind(name, referenceable);
        Object result = namingContext.lookup(name);
        assertEquals(referenceable.addr, result);

        //the same with security permissions
        name = new CompositeName("securitytest");
        testActionPermission(JndiPermission.ACTION_BIND, namingContext, "securitytest", referenceable);
        result = testActionPermission(JndiPermission.ACTION_LOOKUP, namingContext, "securitytest");
        assertEquals(referenceable.addr, result);
    }

    @Test
    public void testUnbind() throws Exception {
        final Name name = new CompositeName("test");
        final Object value = new Object();
        namingStore.bind(name, value);
        namingContext.unbind(name);
        try {
            namingStore.lookup(name);
            fail("Should have thrown name not found");
        } catch (NameNotFoundException expect) {}

        //the same with security permissions
        testActionPermission(JndiPermission.ACTION_BIND, namingContext, "test", value);
        testActionPermission(JndiPermission.ACTION_UNBIND, namingContext, "test");
        try {
            namingStore.lookup(name);
            fail("Should have thrown name not found");
        } catch (NameNotFoundException expect) {}
    }

    @Test
    public void testCreateSubcontext() throws Exception {
        assertTrue(namingContext.createSubcontext(new CompositeName("test")) instanceof NamingContext);

        //the same with security permissions
        assertTrue(testActionPermission(JndiPermission.ACTION_CREATE_SUBCONTEXT, namingContext, "securitytest") instanceof NamingContext);
    }

    @Test
    public void testRebind() throws Exception {
        final Name name = new CompositeName("test");
        final Object value = new Object();
        namingStore.bind(name, value);
        Object newValue = new Object();
        namingContext.rebind(name, newValue);
        assertEquals(newValue, namingStore.lookup(name));

        //the same with security permissions
        newValue = new Object();
        testActionPermission(JndiPermission.ACTION_REBIND, namingContext, "test", newValue);
        assertEquals(newValue, namingStore.lookup(name));
    }

    @Test
    public void testRebindReferenceable() throws Exception {
        final Name name = new CompositeName("test");
        final TestObjectReferenceable referenceable = new TestObjectReferenceable("addr");
        namingContext.bind(name, referenceable);
        TestObjectReferenceable newReferenceable = new TestObjectReferenceable("newAddr");
        namingContext.rebind(name, newReferenceable);
        Object result = namingContext.lookup(name);
        assertEquals(newReferenceable.addr, result);

        //the same with security permissions
        newReferenceable = new TestObjectReferenceable("yetAnotherNewAddr");
        testActionPermission(JndiPermission.ACTION_REBIND, namingContext, "test", newReferenceable);
        result = namingContext.lookup(name);
        assertEquals(newReferenceable.addr, result);
    }

    @Test
    public void testListNameNotFound() throws Exception {
        try {
            namingContext.list(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        //the same with security permissions
        try {
            testActionPermission(JndiPermission.ACTION_LIST, namingContext, "test");
            fail("Should have thrown and NameNotFoundException with appropriate permissions");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testList() throws Exception {
        bindList();

        NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName());
        checkListResults(results);

        //the same with security permissions
        results = (NamingEnumeration<NameClassPair>) testActionPermission(JndiPermission.ACTION_LIST, namingContext, null);
        checkListResults(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListWithContinuation() throws Exception {
        bindListWithContinuations();

        NamingEnumeration<NameClassPair> results = namingContext.list(new CompositeName("comp"));
        checkListWithContinuationsResults(results);

        //the same with security permissions
        results = (NamingEnumeration<NameClassPair>) testActionPermission(JndiPermission.ACTION_LIST, Arrays.asList(
                new JndiPermission("test", "list")), namingContext, "comp");

        checkListWithContinuationsResults(results);
    }

    @Test
    public void testListBindingsNameNotFound() throws Exception {
        try {
            namingContext.listBindings(new CompositeName("test"));
            fail("Should have thrown and NameNotFoundException");
        } catch (NameNotFoundException expected) {
        }

        //the same with security permissions
        try {
            testActionPermission(JndiPermission.ACTION_LIST_BINDINGS, namingContext, "test");
            fail("Should have thrown and NameNotFoundException with appropriate permissions");
        } catch (NameNotFoundException expected) {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListBindings() throws Exception {
        bindList();

        NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName());
        checkListResults(results);

        //the same with security permissions
        results = (NamingEnumeration<Binding>) testActionPermission(JndiPermission.ACTION_LIST_BINDINGS, namingContext, null);
        checkListResults(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListBindingsWithContinuation() throws Exception {
        bindListWithContinuations();

        NamingEnumeration<Binding> results = namingContext.listBindings(new CompositeName("comp"));
        checkListWithContinuationsResults(results);

        //the same with security permissions
        results = (NamingEnumeration<Binding>) testActionPermission(JndiPermission.ACTION_LIST_BINDINGS, Arrays.asList(
                new JndiPermission("test", "listBindings")), namingContext, "comp");

        checkListWithContinuationsResults(results);
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

    public static class TestObjectReferenceable implements Referenceable, Serializable {

        private static final long serialVersionUID = 1L;

        private String addr;

        public TestObjectReferenceable(String addr) {
            this.addr = addr;
        }

        @Override
        public Reference getReference() throws NamingException {
            return new Reference(String.class.getName(), new StringRefAddr("blah", addr), TestObjectFactory.class.getName(),
                    null);
        }

    }

    private void bindList() throws NamingException {
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
    }

    private void bindListWithContinuations() throws NamingException {
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
    }

    private void checkListResults(NamingEnumeration<? extends NameClassPair> results) throws NamingException {
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

    private void checkListWithContinuationsResults(NamingEnumeration<? extends NameClassPair> results) throws NamingException {
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

}
