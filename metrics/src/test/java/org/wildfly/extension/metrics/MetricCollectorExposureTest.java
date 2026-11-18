/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/** Verifies resource discovery respects the configured subsystem exposure filter. */
public class MetricCollectorExposureTest {

    /** Verifies caller mutations cannot expose subsystems excluded when the collector was constructed. */
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

        List<String> exposedSubsystems = new ArrayList<>(List.of("undertow"));
        MetricCollector collector = new MetricCollector(null, null, processStateNotifier,
                task -> submittedTasks.incrementAndGet(), false, exposedSubsystems);
        try {
            exposedSubsystems.clear();
            exposedSubsystems.add("messaging-activemq");
            collector.collectRootResourceMetrics(Resource.Factory.create(), registration, null,
                    new MetricRegistration(new WildFlyMetricRegistry()));
            collector.resourceAdded(PathAddress.pathAddress("subsystem", "messaging-activemq"));

            assertEquals("Unexposed resources must not start background discovery", 0, submittedTasks.get());
            collector.resourceAdded(PathAddress.pathAddress("subsystem", "undertow"));
            assertEquals("Originally exposed resources must still start background discovery", 1, submittedTasks.get());
        } finally {
            collector.stop();
        }
    }

    /** Verifies background discovery requests only the added resource's model. */
    @Test
    public void discoversAddedResourceWithoutRecursiveRead() {
        AtomicReference<ModelNode> operation = new AtomicReference<>();
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();
        LocalModelControllerClient discoveryClient = (LocalModelControllerClient) Proxy.newProxyInstance(
                LocalModelControllerClient.class.getClassLoader(),
                new Class<?>[] {LocalModelControllerClient.class},
                (proxy, method, arguments) -> {
                    if ("execute".equals(method.getName())) {
                        operation.set((ModelNode) arguments[0]);
                        ModelNode result = new ModelNode();
                        result.get("outcome").set("success");
                        result.get("result").setEmptyObject();
                        return result;
                    }
                    return null;
                });
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

        MetricCollector collector = new MetricCollector(discoveryClient, discoveryClient, processStateNotifier,
                submittedTask::set, true, List.of());
        try {
            collector.collectRootResourceMetrics(Resource.Factory.create(), registration, null,
                    new MetricRegistration(new WildFlyMetricRegistry()));
            collector.resourceAdded(PathAddress.pathAddress("subsystem", "messaging-activemq"));
            submittedTask.get().run();

            assertFalse("Background discovery must not read recursively", operation.get().get("recursive").asBoolean());
        } finally {
            collector.stop();
        }
    }
}
