/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.jboss.tm.XAResourceRecoveryRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies the supplier-collection behaviour of {@link WildFlyRecoveryRegistry}.
 * <p>
 * Every messaging service that needs XA recovery (pooled-connection-factory, external
 * pooled-connection-factory, JMS bridge) registers a {@code Supplier<XAResourceRecoveryRegistry>}
 * obtained from its {@code ServiceBuilder.requires(...)}. Such a supplier returns a non-null value
 * only while its owning service is up. Recovery must resolve the registry as long as <em>any</em>
 * contributing service is up, and must not be pinned to a single service that may be down after a
 * connection failure and restart (the WFLY-21659 regression). Since the capability resolves to a
 * single global registry, any non-null supplier returns the same instance.
 */
public class WildFlyRecoveryRegistryTestCase {

    @Before
    public void setUp() {
        WildFlyRecoveryRegistry.clearSuppliers();
    }

    @After
    public void tearDown() {
        WildFlyRecoveryRegistry.clearSuppliers();
    }

    @Test(expected = IllegalStateException.class)
    public void constructionFailsWhenNoSupplierIsRegistered() {
        new WildFlyRecoveryRegistry();
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void constructionFailsWhenTheOnlySupplierIsDown() {
        // A supplier whose owning service is down returns null.
        Supplier<XAResourceRecoveryRegistry> downSupplier = mock(Supplier.class);
        when(downSupplier.get()).thenReturn(null);
        WildFlyRecoveryRegistry.registerSupplier(downSupplier);

        new WildFlyRecoveryRegistry();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolvesThroughALiveSupplierWhenAnotherIsDown() {
        // This is the regression guard: even though the first-registered service is down, recovery must
        // still resolve the registry through a second service that is up.
        XAResourceRecoveryRegistry expected = mock(XAResourceRecoveryRegistry.class);

        Supplier<XAResourceRecoveryRegistry> downSupplier = mock(Supplier.class);
        when(downSupplier.get()).thenReturn(null);
        Supplier<XAResourceRecoveryRegistry> liveSupplier = mock(Supplier.class);
        when(liveSupplier.get()).thenReturn(expected);

        WildFlyRecoveryRegistry.registerSupplier(downSupplier);
        WildFlyRecoveryRegistry.registerSupplier(liveSupplier);

        WildFlyRecoveryRegistry registry = new WildFlyRecoveryRegistry();
        assertNotNull(registry.getTMRegistry());
        assertSame("The registry must be resolved through the live supplier", expected, registry.getTMRegistry());
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void deregisteredSupplierNoLongerContributes() {
        XAResourceRecoveryRegistry registry = mock(XAResourceRecoveryRegistry.class);
        Supplier<XAResourceRecoveryRegistry> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(registry);

        WildFlyRecoveryRegistry.registerSupplier(supplier);
        WildFlyRecoveryRegistry.deregisterSupplier(supplier);

        // With its only supplier deregistered, the registry can no longer be resolved.
        new WildFlyRecoveryRegistry();
    }
}
