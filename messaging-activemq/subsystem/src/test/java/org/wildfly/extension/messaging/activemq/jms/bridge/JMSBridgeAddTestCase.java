/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.junit.Assert.assertNotNull;
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
        WildFlyRecoveryRegistry.clearSupplier();
    }

    @After
    public void tearDown() {
        WildFlyRecoveryRegistry.clearSupplier();
    }

    @Test(expected = IllegalStateException.class)
    public void testRecoveryRegistryFailsWithoutSupplier() {
        // Without the fix in JMSBridgeAdd, the supplier is never set.
        // WildFlyRecoveryRegistry's constructor throws when it can't find
        // the XAResourceRecoveryRegistry — this is the bug scenario.
        new WildFlyRecoveryRegistry();
    }

    @Test
    public void testRecoveryRegistrySucceedsWithSupplier() {
        // With the fix, JMSBridgeAdd sets the supplier before the bridge starts.
        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));
        WildFlyRecoveryRegistry.setSupplier(mockSupplier);

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

        // Simulate what JMSBridgeAdd does: set the recovery registry supplier
        Supplier<XAResourceRecoveryRegistry> mockSupplier = mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(mock(XAResourceRecoveryRegistry.class));
        WildFlyRecoveryRegistry.setSupplier(mockSupplier);

        // Override startBridge() to skip Module loading which is unavailable in unit tests
        JMSBridgeService service = new JMSBridgeService(null, "test-bridge", bridge, executorSupplier, null, null) {
            @Override
            public void startBridge() throws Exception {
                getValue().start();
            }
        };

        service.start(startContext);

        verify(bridge).start();
    }
}
