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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class WritableServiceBasedNamingStoreTestCase {
    private ServiceContainer container;
    private WritableServiceBasedNamingStore store;

    @Before
    public void setup() throws Exception {
        container = ServiceContainer.Factory.create();
        store = new WritableServiceBasedNamingStore(container, ContextNames.JAVA_CONTEXT_SERVICE_NAME);
        final CountDownLatch latch = new CountDownLatch(1);
        container.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService(store))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new AbstractServiceListener<NamingStore>() {
                    public void transition(ServiceController<? extends NamingStore> controller, ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                latch.countDown();
                                break;
                            }
                            case STARTING_to_START_FAILED: {
                                latch.countDown();
                                fail("Did not install store service - " + controller.getStartException().getMessage());
                                break;
                            }
                        }
                    }
                })
                .install();
        latch.await(10, TimeUnit.SECONDS);
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
        store = null;
    }

    @Test
    public void testBindNoOwner() throws Exception {
        try {
            store.bind(new CompositeName("test"), new Object());
            fail("Should have failed with a read-only context exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testBind() throws Exception {
        final Name name = new CompositeName("test");
        final Object value = new Object();
        WritableServiceBasedNamingStore.pushOwner(container);
        try {
            store.bind(name, value);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        assertEquals(value, store.lookup(name));
    }

    @Test
    public void testBindNested() throws Exception {
        final Name name = new CompositeName("nested/test");
        final Object value = new Object();
        WritableServiceBasedNamingStore.pushOwner(container);
        try {
            store.bind(name, value);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        assertEquals(value, store.lookup(name));
    }

    @Test
    public void testUnbind() throws Exception {
        final Name name = new CompositeName("test");
        final Object value = new Object();
        WritableServiceBasedNamingStore.pushOwner(container);
        try {
            store.bind(name, value);
            store.unbind(name);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        try {
            store.lookup(name);
            fail("Should have thrown name not found");
        } catch (NameNotFoundException expect) {
        }
    }

    @Test
    public void testUnBindNoOwner() throws Exception {
        try {
            store.unbind(new CompositeName("test"));
            fail("Should have failed with a read-only context exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testCreateSubcontext() throws Exception {
        WritableServiceBasedNamingStore.pushOwner(container);
        try {
            assertTrue(((NamingContext) store.createSubcontext(new CompositeName("test"))).getNamingStore() instanceof WritableServiceBasedNamingStore);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
    }

    @Test
    public void testCreateSubContextNoOwner() throws Exception {
        try {
            store.createSubcontext(new CompositeName("test"));
            fail("Should have failed with a read-only context exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testRebind() throws Exception {
        final Name name = new CompositeName("test");
        final Object value = new Object();
        final Object newValue = new Object();
        WritableServiceBasedNamingStore.pushOwner(container);
        try {
            store.bind(name, value);
            store.rebind(name, newValue);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        assertEquals(newValue, store.lookup(name));
    }

    @Test
    public void testRebindNoOwner() throws Exception {
        try {
            store.rebind(new CompositeName("test"), new Object());
            fail("Should have failed with a read-only context exception");
        } catch (UnsupportedOperationException expected) {
        }
    }
}
