/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.junit.Test;

/** Verifies resource discovery respects the configured subsystem exposure filter. */
public class MetricCollectorExposureTest {

    /** Verifies an unexposed subsystem is not submitted to the asynchronous discovery executor. */
    @Test
    public void doesNotQueueUnexposedSubsystems() {
        AtomicInteger submittedTasks = new AtomicInteger();
        ProcessStateNotifier processStateNotifier = (ProcessStateNotifier) Proxy.newProxyInstance(
                ProcessStateNotifier.class.getClassLoader(),
                new Class<?>[] {ProcessStateNotifier.class},
                (proxy, method, arguments) -> "getCurrentState".equals(method.getName())
                        ? ControlledProcessState.State.RUNNING : null);
        ImmutableManagementResourceRegistration registration =
                (ImmutableManagementResourceRegistration) Proxy.newProxyInstance(
                        ImmutableManagementResourceRegistration.class.getClassLoader(),
                        new Class<?>[] {ImmutableManagementResourceRegistration.class},
                        (proxy, method, arguments) -> null);

        MetricCollector collector = new MetricCollector(null, null, processStateNotifier,
                task -> submittedTasks.incrementAndGet(), false, List.of("undertow"));
        try {
            collector.collectRootResourceMetrics(Resource.Factory.create(), registration, null,
                    new MetricRegistration(new WildFlyMetricRegistry()));
            collector.resourceAdded(PathAddress.pathAddress("subsystem", "messaging-activemq"));

            assertEquals("Unexposed resources must not start background discovery", 0, submittedTasks.get());
        } finally {
            collector.stop();
        }
    }
}
