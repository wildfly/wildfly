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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameNotFoundException;

import org.jboss.as.naming.JndiPermission.Action;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.deployment.RuntimeBindReleaseService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.naming.SecurityHelper.testActionWithPermission;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Bailey
 * @author Eduardo Martins
 */
public class WritableServiceBasedNamingStoreTestCase {
    private ServiceContainer container;
    private WritableServiceBasedNamingStore store;
    private static final ServiceName OWNER_FOO = ServiceName.of("Foo");
    private static final ServiceName OWNER_BAR = ServiceName.of("Bar");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setup() throws Exception {
        container = ServiceContainer.Factory.create();
        installOwnerService(OWNER_FOO);
        installOwnerService(OWNER_BAR);
        store = new WritableServiceBasedNamingStore(container, ContextNames.JAVA_CONTEXT_SERVICE_NAME,container.subTarget());
        final CountDownLatch latch2 = new CountDownLatch(1);
        container.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService(store))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new AbstractServiceListener<NamingStore>() {
                    public void transition(ServiceController<? extends NamingStore> controller, ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                latch2.countDown();
                                break;
                            }
                            case STARTING_to_START_FAILED: {
                                latch2.countDown();
                                fail("Did not install store service - " + controller.getStartException().getMessage());
                                break;
                            }
                            default:
                                break;
                        }
                    }
                })
                .install();
        latch2.await(10, TimeUnit.SECONDS);
    }

    private void installOwnerService(ServiceName owner) throws InterruptedException {
        final CountDownLatch latch1 = new CountDownLatch(1);
        container.addService(JndiNamingDependencyProcessor.serviceName(owner), new RuntimeBindReleaseService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new AbstractServiceListener() {
                    public void transition(ServiceController controller, ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                latch1.countDown();
                                break;
                            }
                            case STARTING_to_START_FAILED: {
                                latch1.countDown();
                                fail("Did not install store service - " + controller.getStartException().getMessage());
                                break;
                            }
                            default:
                                break;
                        }
                    }
                })
                .install();
        latch1.await(10, TimeUnit.SECONDS);
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
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
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
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
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
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
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
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
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
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
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

    /**
     * Binds an entry and then do lookups with several permissions
     * @throws Exception
     */
    @Test
    public void testPermissions() throws Exception {

        final NamingContext namingContext = new NamingContext(store, null);
        final String name = "a/b";
        final Object value = new Object();
        ArrayList<JndiPermission> permissions = new ArrayList<JndiPermission>();

        // simple bind test, note that permission must have absolute path
        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
        try {
            permissions.add(new JndiPermission(store.getBaseName()+"/"+name,"bind,list,listBindings"));
            store.bind(new CompositeName(name), value);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }

        // all of these lookup should work
        permissions.set(0,new JndiPermission(store.getBaseName()+"/"+name,Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, name));
        permissions.set(0,new JndiPermission(store.getBaseName()+"/-",Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, name));
                permissions.set(0,new JndiPermission(store.getBaseName()+"/a/*",Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, name));
        permissions.set(0,new JndiPermission(store.getBaseName()+"/a/-",Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, name));
        permissions.set(0,new JndiPermission("<<ALL BINDINGS>>",Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, name));
        permissions.set(0,new JndiPermission(store.getBaseName()+"/"+name,Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, namingContext, store.getBaseName()+"/"+name));
        NamingContext aNamingContext = (NamingContext) namingContext.lookup("a");
        permissions.set(0,new JndiPermission(store.getBaseName()+"/"+name,Action.LOOKUP));
        assertEquals(value, testActionWithPermission(Action.LOOKUP, permissions, aNamingContext, "b"));
        // this lookup should not work, no permission
        try {
            testActionWithPermission(Action.LOOKUP, Collections.<JndiPermission>emptyList(), namingContext, name);
            fail("Should have failed due to missing permission");
        } catch (AccessControlException e) {

        }
        // a permission which only allows entries in store.getBaseName()
        try {
            permissions.set(0,new JndiPermission(store.getBaseName()+"/*",Action.LOOKUP));
            testActionWithPermission(Action.LOOKUP, permissions, namingContext, name);
            fail("Should have failed due to missing permission");
        } catch (AccessControlException e) {

        }
        // permissions which are not absolute paths (do not include store base name, i.e. java:)
        try {
            permissions.set(0,new JndiPermission(name,Action.LOOKUP));
            testActionWithPermission(Action.LOOKUP, permissions, namingContext, name);
            fail("Should have failed due to missing permission");
        } catch (AccessControlException e) {

        }
        try {
            permissions.set(0,new JndiPermission("/"+name,Action.LOOKUP));
            testActionWithPermission(Action.LOOKUP, permissions, namingContext, name);
            fail("Should have failed due to missing permission");
        } catch (AccessControlException e) {

        }
        try {
            permissions.set(0,new JndiPermission("/-",Action.LOOKUP));
            testActionWithPermission(Action.LOOKUP, permissions, namingContext, name);
            fail("Should have failed due to missing permission");
        } catch (AccessControlException e) {

        }
    }

    @Test
    public void testOwnerBindingReferences() throws Exception {
        final Name name = new CompositeName("test");
        final ServiceName serviceName = store.buildServiceName(name);
        final Object value = new Object();

        // ensure bind does not exists
        try {
            store.lookup(name);
            fail("Should have thrown name not found");
        } catch (NameNotFoundException expect) {
        }
        // ensure the Foo's RuntimeBindReleaseService has no reference to the future bind
        final Set<ServiceName> duBindingReferences = (Set<ServiceName>) container.getService(JndiNamingDependencyProcessor.serviceName(OWNER_FOO)).getValue();
        assertFalse(duBindingReferences.contains(serviceName));

        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
        try {
            store.bind(name, value);
            // Foo's RuntimeBindReleaseService should now have a reference to the new bind
            assertTrue(duBindingReferences.contains(serviceName));

            store.rebind(name, value);
            // after rebind, Foo's RuntimeBindReleaseService should continue to have a reference to the bind
            assertTrue(duBindingReferences.contains(serviceName));

            store.unbind(name);
            // Foo's RuntimeBindReleaseService reference to the bind should have been removed
            assertFalse(duBindingReferences.contains(serviceName));
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
    }

    @Test
    public void testMultipleOwnersBindingReferences() throws Exception {
        final Name name = new CompositeName("test");
        final ServiceName serviceName = store.buildServiceName(name);
        final Object value = new Object();

        // ensure bind does not exists
        try {
            store.lookup(name);
            fail("Should have thrown name not found");
        } catch (NameNotFoundException expect) {
        }
        // ensure the owners RuntimeBindReleaseService have no reference to the future bind
        final Set<ServiceName> fooDuBindingReferences = (Set<ServiceName>) container.getService(JndiNamingDependencyProcessor.serviceName(OWNER_FOO)).getValue();
        assertFalse(fooDuBindingReferences.contains(serviceName));
        final Set<ServiceName> barDuBindingReferences = (Set<ServiceName>) container.getService(JndiNamingDependencyProcessor.serviceName(OWNER_BAR)).getValue();
        assertFalse(barDuBindingReferences.contains(serviceName));

        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
        try {
            store.bind(name, value);
            // Foo's RuntimeBindReleaseService should now have a reference to the new bind
            assertTrue(fooDuBindingReferences.contains(serviceName));
            // Bar's RuntimeBindReleaseService reference to the bind should not exist
            assertFalse(barDuBindingReferences.contains(serviceName));
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }

        WritableServiceBasedNamingStore.pushOwner(OWNER_BAR);
        try {
            store.rebind(name, value);
            // after rebind, Foo's RuntimeBindReleaseService reference to the bind should have been removed
            assertFalse(fooDuBindingReferences.contains(serviceName));
            // after rebind, Bar's RuntimeBindReleaseService reference to the bind should now exist
            assertTrue(barDuBindingReferences.contains(serviceName));
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }

        WritableServiceBasedNamingStore.pushOwner(OWNER_FOO);
        try {
            store.unbind(name);
            // after unbind, Foo's RuntimeBindReleaseService reference to the bind should still not exist
            assertFalse(fooDuBindingReferences.contains(serviceName));
            // after unbind, Bar's RuntimeBindReleaseService reference to the bind should have been removed
            assertFalse(barDuBindingReferences.contains(serviceName));
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
    }
}
