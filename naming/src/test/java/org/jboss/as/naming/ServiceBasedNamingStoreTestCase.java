/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import com.sun.xml.internal.rngom.nc.NameClass;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NameClassPair;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Values;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ServiceBasedNamingStoreTestCase {
    private final ServiceContainer container = ServiceContainer.Factory.create();
    private final ServiceBasedNamingStore store = new ServiceBasedNamingStore(container, ServiceName.JBOSS);

    @Test
    public void testLookupBase() throws Exception {
        final Object obj = store.lookup(new CompositeName());
        assertNotNull(obj);
    }

    @Test
    public void testLookupBinding() throws Exception {
        final ServiceName bindingName = ServiceName.JBOSS.append("foo", "bar");
        final Object value = new Object();
        bindObject(bindingName, value);

        final Object obj = store.lookup(new CompositeName("foo/bar"));
        assertNotNull(obj);
        assertEquals(value, obj);
    }

    @Test
    public void testLookupParentContext() throws Exception {
        final ServiceName bindingName = ServiceName.JBOSS.append("foo", "bar");
        store.add(bindingName);

        final Object obj = store.lookup(new CompositeName("foo"));
        assertNotNull(obj);
        assertTrue(obj instanceof Context);
    }

    @Test
    public void testLookupNestedContext() throws Exception {
        final ServiceName bindingName = ServiceName.JBOSS.append("foo", "bar", "baz", "TestBean");
        store.add(bindingName);
        store.add(ServiceName.JBOSS.append("foos", "bar"));
        store.add(ServiceName.JBOSS.append("fo", "bar"));
        store.add(ServiceName.JBOSS.append("foo", "ba"));
        store.add(ServiceName.JBOSS.append("foo", "bart"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "ba"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "bazt"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "art"));

        Object obj = store.lookup(new CompositeName("foo"));
        assertNotNull(obj);
        assertTrue(obj instanceof Context);

        obj = Context.class.cast(obj).lookup(new CompositeName("bar"));
        assertNotNull(obj);
        assertTrue(obj instanceof Context);

        obj = Context.class.cast(obj).lookup(new CompositeName("baz"));
        assertNotNull(obj);
        assertTrue(obj instanceof Context);
    }

    @Test
    public void testLookupBindingUsingNestedContext() throws Exception {
        final ServiceName bindingName = ServiceName.JBOSS.append("foo", "bar", "baz", "TestBean");
        final Object value = new Object();
        bindObject(bindingName, value);

        Object context = store.lookup(new CompositeName("foo"));
        assertNotNull(context);
        assertTrue(context instanceof Context);

        Object obj = Context.class.cast(context).lookup(new CompositeName("bar/baz/TestBean"));
        assertNotNull(obj);
        assertEquals(value, obj);

        context = Context.class.cast(context).lookup(new CompositeName("bar"));
        obj = Context.class.cast(context).lookup(new CompositeName("baz/TestBean"));
        assertNotNull(obj);
        assertEquals(value, obj);


        context = Context.class.cast(context).lookup(new CompositeName("baz"));
        obj = Context.class.cast(context).lookup(new CompositeName("TestBean"));
        assertNotNull(obj);
        assertEquals(value, obj);
    }


    @Test
    public void testList() throws Exception {
        final Object value = new Object();
        bindObject(ServiceName.JBOSS.append("TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "bar", "TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "bar", "baz", "TestBean"), value);

        store.add(ServiceName.JBOSS.append("foos", "bar"));
        store.add(ServiceName.JBOSS.append("fo", "bar"));
        store.add(ServiceName.JBOSS.append("foo", "ba", "baz"));
        store.add(ServiceName.JBOSS.append("foo", "bart", "baz"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "ba"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "bazt"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "art"));
        store.add(ServiceName.JBOSS.append("other", "one"));

        List<NameClassPair> list = store.list(new CompositeName(""));
        assertEquals(5, list.size());
        assertContains(list, "TestBean", Object.class);
        assertContains(list, "foo", Context.class);
        assertContains(list, "fo", Context.class);
        assertContains(list, "foos", Context.class);
        assertContains(list, "other", Context.class);


        list = store.list(new CompositeName("foo"));
        assertEquals(4, list.size());
        assertContains(list, "TestBean", Object.class);
        assertContains(list, "ba", Context.class);
        assertContains(list, "bart", Context.class);
        assertContains(list, "bar", Context.class);
    }

    @Test
    public void testListBindings() throws Exception {
        final Object value = new Object();
        bindObject(ServiceName.JBOSS.append("TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "bar", "TestBean"), value);
        bindObject(ServiceName.JBOSS.append("foo", "bar", "baz", "TestBean"), value);

        store.add(ServiceName.JBOSS.append("foos", "bar"));
        store.add(ServiceName.JBOSS.append("fo", "bar"));
        store.add(ServiceName.JBOSS.append("foo", "ba", "baz"));
        store.add(ServiceName.JBOSS.append("foo", "bart", "baz"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "ba"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "bazt"));
        store.add(ServiceName.JBOSS.append("foo", "bar", "art"));
        store.add(ServiceName.JBOSS.append("other", "one"));

        List<Binding> list = store.listBindings(new CompositeName(""));
        assertEquals(5, list.size());
        assertContains(list, "TestBean", Object.class);
        assertContains(list, "foo", NamingContext.class);
        assertContains(list, "fo", NamingContext.class);
        assertContains(list, "foos", NamingContext.class);
        assertContains(list, "other", NamingContext.class);


        list = store.listBindings(new CompositeName("foo"));
        assertEquals(4, list.size());
        assertContains(list, "TestBean", Object.class);
        assertContains(list, "ba", NamingContext.class);
        assertContains(list, "bart", NamingContext.class);
        assertContains(list, "bar", NamingContext.class);

        for (Binding binding : list) {
            if (binding.getName().equals("bar")) {
                final Object bean = Context.class.cast(binding.getObject()).lookup("TestBean");
                assertNotNull(bean);
                assertEquals(value, bean);
            }
        }
    }

    private void assertContains(final List<? extends NameClassPair> list, String name, Class<?> type) {
        for (NameClassPair value : list) {
            if (value instanceof Binding) {
                assertNotNull(Binding.class.cast(value).getObject());
            }
            if (value.getName().equals(name) && value.getClassName().equals(type.getName())) {
                return;
            }
        }
        fail("Child [" + name + "] not found in [" + list + "]");
    }

    private void bindObject(final ServiceName serviceName, final Object value) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        container.addService(serviceName, new Service<ManagedReferenceFactory>() {
            public void start(StartContext context) throws StartException {
                store.add(serviceName);
                latch.countDown();
            }

            public void stop(StopContext context) {
            }

            public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
                return new ValueManagedReferenceFactory(Values.immediateValue(value));
            }
        }).install();
        latch.await();
    }
}
