/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

/** Verifies that Micrometer shutdown does not close an active discovery client. */
public class MicrometerServiceShutdownTest {

    private static final long TIMEOUT_SECONDS = 5;

    /** Verifies that client closure waits for an in-flight discovery operation. */
    @Test
    public void shutdownWaitsForDiscoveryOperation() throws Exception {
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();
        Executor executor = submittedTask::set;
        BlockingClient discoveryClient = new BlockingClient();
        MicrometerService service = createService(executor, discoveryClient);
        ResourceMetricsRetryQueue queue = resourceMetricsRetryQueue(service);

        queue.add(PathAddress.pathAddress("subsystem", "test"));
        Thread worker = new Thread(submittedTask.get()::run);
        worker.start();
        assertTrue("Discovery operation did not start", discoveryClient.started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        Thread stopper = new Thread(service::stop);
        stopper.start();
        assertFalse("Discovery client was closed while execute was active",
                discoveryClient.closeAttempted.await(200, TimeUnit.MILLISECONDS));

        discoveryClient.release.countDown();
        stopper.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertFalse("Discovery client was closed while execute was active",
                discoveryClient.closedDuringExecute.get());
        assertTrue("Discovery client was not closed", discoveryClient.closeAttempted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    /** Creates a Micrometer service with a captured retry worker for the shutdown test. */
    private static MicrometerService createService(Executor executor, LocalModelControllerClient discoveryClient)
            throws Exception {
        Constructor<MicrometerService> constructor = MicrometerService.class.getDeclaredConstructor(
                WildFlyMicrometerConfig.class, LocalModelControllerClient.class, LocalModelControllerClient.class,
                org.jboss.as.controller.ProcessStateNotifier.class, WildFlyCompositeRegistry.class, Executor.class);
        constructor.setAccessible(true);
        MicrometerService service = constructor.newInstance(null, new NoopClient(), discoveryClient,
                null, new WildFlyCompositeRegistry(), executor);
        Object registration = Proxy.newProxyInstance(
                ImmutableManagementResourceRegistration.class.getClassLoader(),
                new Class<?>[] {ImmutableManagementResourceRegistration.class},
                (proxy, method, args) -> null);
        setField(service, "modelRegistration", registration);
        setField(service, "modelMetrics", new MetricRegistration(new WildFlyCompositeRegistry()));
        return service;
    }

    /** Returns the retry queue owned by the service under test. */
    private static ResourceMetricsRetryQueue resourceMetricsRetryQueue(MicrometerService service) throws Exception {
        Field field = MicrometerService.class.getDeclaredField("resourceMetricsRetryQueue");
        field.setAccessible(true);
        return (ResourceMetricsRetryQueue) field.get(service);
    }

    /** Sets a private service field for the narrowly scoped lifecycle test fixture. */
    private static void setField(MicrometerService service, String name, Object value) throws Exception {
        Field field = MicrometerService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    /** Client that blocks discovery until the test explicitly releases it. */
    private static class BlockingClient implements LocalModelControllerClient {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch closeAttempted = new CountDownLatch(1);
        private final AtomicBoolean executing = new AtomicBoolean();
        private final AtomicBoolean closedDuringExecute = new AtomicBoolean();

        /** Blocks until shutdown releases the simulated management operation. */
        @Override
        public ModelNode execute(ModelNode operation) {
            executing.set(true);
            started.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            executing.set(false);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return new ModelNode();
        }

        /** Returns an empty response for unused synchronous operation APIs. */
        @Override
        public OperationResponse executeOperation(Operation operation, OperationMessageHandler messageHandler) {
            return OperationResponse.Factory.createSimple(new ModelNode());
        }

        /** Returns an already completed empty response for the unused async API. */
        @Override
        public CompletableFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
            return CompletableFuture.completedFuture(new ModelNode());
        }

        /** Returns an already completed empty response for the unused async operation API. */
        @Override
        public CompletableFuture<OperationResponse> executeOperationAsync(Operation operation,
                                                                           OperationMessageHandler messageHandler) {
            return CompletableFuture.completedFuture(OperationResponse.Factory.createSimple(new ModelNode()));
        }

        /** Records whether shutdown attempted closure during the simulated operation. */
        @Override
        public void close() {
            if (executing.get()) {
                closedDuringExecute.set(true);
            }
            closeAttempted.countDown();
        }
    }

    /** Client used for the unrelated model metrics dependency. */
    private static final class NoopClient extends BlockingClient {
        /** Returns an empty response because model metrics are not part of this test. */
        @Override
        public ModelNode execute(ModelNode operation) {
            return new ModelNode();
        }
    }
}
