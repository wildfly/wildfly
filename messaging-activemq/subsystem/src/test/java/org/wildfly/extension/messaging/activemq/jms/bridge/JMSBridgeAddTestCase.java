/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extension.messaging.activemq.jms.WildFlyRecoveryRegistry;

/**
 * Verifies that the JMS bridge XA recovery registration requires the
 * WildFlyRecoveryRegistry supplier to be configured by JMSBridgeAdd.
 */
public class JMSBridgeAddTestCase {

    @Before
    public void setUp() {
        WildFlyRecoveryRegistry.clearSuppliers();
    }

    @After
    public void tearDown() {
        WildFlyRecoveryRegistry.clearSuppliers();
    }

    @Test(expected = IllegalStateException.class)
    public void testRecoveryRegistryFailsWithoutSupplier() {
        // Without any messaging service registering a supplier, WildFlyRecoveryRegistry's
        // constructor throws when it can't find the XAResourceRecoveryRegistry.
        new WildFlyRecoveryRegistry();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRecoveryRegistrySucceedsWithSupplier() {
        // With the fix, JMSBridgeAdd registers the supplier before the bridge starts.
        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));
        WildFlyRecoveryRegistry.registerSupplier(mockSupplier);

        WildFlyRecoveryRegistry registry = new WildFlyRecoveryRegistry();
        assertNotNull(registry.getTMRegistry());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBridgeStartsWhenRecoveryRegistrySupplierIsSet() throws Exception {
        JMSBridge bridge = mock(JMSBridge.class);
        ServiceController<?> controller = mock(ServiceController.class);
        StartContext startContext = mock(StartContext.class);
        ExecutorService executor = mock(ExecutorService.class);
        Supplier<ExecutorService> executorSupplier = mock(Supplier.class);

        when(startContext.getController()).thenReturn((ServiceController) controller);
        when(executorSupplier.get()).thenReturn(executor);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));

        // Override startBridge() to skip Module loading which is unavailable in unit tests
        JMSBridgeService service = new JMSBridgeService(null, "test-bridge", bridge, executorSupplier, null, null, mockSupplier) {
            @Override
            public void startBridge() throws Exception {
                getValue().start();
            }
        };

        service.start(startContext);

        verify(bridge).start();
        // start() must register the supplier itself (JMSBridgeAdd no longer does), so recovery resolves.
        assertNotNull(new WildFlyRecoveryRegistry().getTMRegistry());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBridgeReRegistersRecoveryRegistrySupplierOnRestart() throws Exception {
        JMSBridge bridge = mock(JMSBridge.class);
        ServiceController<?> controller = mock(ServiceController.class);
        StartContext startContext = mock(StartContext.class);
        StopContext stopContext = mock(StopContext.class);
        ExecutorService executor = mock(ExecutorService.class);
        Supplier<ExecutorService> executorSupplier = mock(Supplier.class);

        when(startContext.getController()).thenReturn((ServiceController) controller);
        when(executorSupplier.get()).thenReturn(executor);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));

        // The bridge is the only recovery registry contributor; nothing is pre-registered here.
        JMSBridgeService service = new JMSBridgeService(null, "test-bridge", bridge, executorSupplier, null, null, mockSupplier) {
            @Override
            public void startBridge() throws Exception {
                getValue().start();
            }
        };

        // First start: the supplier is registered and recovery resolves.
        service.start(startContext);
        assertNotNull(new WildFlyRecoveryRegistry().getTMRegistry());

        // Stop: the supplier is deregistered and, being the only one, recovery no longer resolves.
        service.stop(stopContext);
        try {
            new WildFlyRecoveryRegistry();
            fail("Expected IllegalStateException after the bridge deregistered its only recovery registry supplier on stop");
        } catch (IllegalStateException expected) {
            // expected
        }

        // Restart without re-running JMSBridgeAdd: start() must re-register so recovery resolves again.
        service.start(startContext);
        assertNotNull(new WildFlyRecoveryRegistry().getTMRegistry());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBridgeStopDeregistersRecoveryRegistrySupplier() {
        JMSBridge bridge = mock(JMSBridge.class);
        StopContext stopContext = mock(StopContext.class);
        ExecutorService executor = mock(ExecutorService.class);
        Supplier<ExecutorService> executorSupplier = mock(Supplier.class);
        when(executorSupplier.get()).thenReturn(executor);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        // The bridge's recovery registry supplier is the only registered one.
        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));
        WildFlyRecoveryRegistry.registerSupplier(mockSupplier);

        JMSBridgeService service = new JMSBridgeService(null, "test-bridge", bridge, executorSupplier, null, null, mockSupplier);

        // While the bridge is up its supplier resolves the registry.
        assertNotNull(new WildFlyRecoveryRegistry().getTMRegistry());

        service.stop(stopContext);

        // After stop the bridge's supplier is deregistered; with no other supplier the registry no longer resolves.
        try {
            new WildFlyRecoveryRegistry();
            fail("Expected IllegalStateException after the bridge deregistered its only recovery registry supplier on stop");
        } catch (IllegalStateException expected) {
            // expected: the deregistered supplier no longer contributes to recovery lookups
        }
    }
}
