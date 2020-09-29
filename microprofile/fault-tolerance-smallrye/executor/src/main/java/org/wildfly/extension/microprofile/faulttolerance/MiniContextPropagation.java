/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.faulttolerance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Miniature context propagation API, adapted from MicroProfile Context Propagation and SmallRye Context Propagation.
 * @author Ladislav Thon (c) 2020 Red Hat, Inc.
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
final class MiniContextPropagation {

    private MiniContextPropagation() {
        // avoid instantiation
    }

    public static ExecutorService executorService(ContextProvider contextProvider, ExecutorService delegate) {
        return new ConPropExecutorService(contextProvider, delegate);
    }

    public static ScheduledExecutorService scheduledExecutorService(ContextProvider contextProvider, ScheduledExecutorService delegate) {
        return new ConPropScheduledExecutorService(contextProvider, delegate);
    }

    @FunctionalInterface
    interface ContextProvider {

        ContextSnapshot capture();

        ContextProvider NOOP = () -> ContextSnapshot.NOOP;

        static ContextProvider compound(ContextProvider... contextProviders) {
            return new CompoundContextProvider(Arrays.asList(contextProviders));
        }
    }

    @FunctionalInterface
    interface ContextSnapshot {

        ActiveContextSnapshot activate();

        ContextSnapshot NOOP = () -> ActiveContextSnapshot.NOOP;
    }

    @FunctionalInterface
    interface ActiveContextSnapshot {

        void deactivate();

        ActiveContextSnapshot NOOP = () -> {
        };
    }

    // ---
    private static final class CompoundContextProvider implements ContextProvider {

        private final List<ContextProvider> contextProviders;

        CompoundContextProvider(List<ContextProvider> contextProviders) {
            this.contextProviders = contextProviders;
        }

        @Override
        public ContextSnapshot capture() {
            List<ContextSnapshot> snapshots = new ArrayList<>();
            for (ContextProvider contextProvider : contextProviders) {
                snapshots.add(contextProvider.capture());
            }

            return () -> {
                List<ActiveContextSnapshot> activeSnapshots = new ArrayList<>();
                for (ContextSnapshot snapshot : snapshots) {
                    activeSnapshots.add(snapshot.activate());
                }

                return () -> {
                    for (int i = activeSnapshots.size() - 1; i >= 0; i--) {
                        activeSnapshots.get(i).deactivate();
                    }
                };
            };
        }
    }

    // ---
    private static final class ConPropRunnable implements Runnable {

        private final ContextSnapshot contextSnapshot;
        private final Runnable delegate;

        ConPropRunnable(ContextSnapshot contextSnapshot, Runnable delegate) {
            this.contextSnapshot = contextSnapshot;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            ActiveContextSnapshot activeSnapshot = contextSnapshot.activate();
            try {
                delegate.run();
            } finally {
                activeSnapshot.deactivate();
            }
        }
    }

    private static final class ConPropCallable<V> implements Callable<V> {

        private final ContextSnapshot contextSnapshot;
        private final Callable<V> delegate;

        ConPropCallable(ContextSnapshot contextSnapshot, Callable<V> delegate) {
            this.contextSnapshot = contextSnapshot;
            this.delegate = delegate;
        }

        @Override
        public V call() throws Exception {
            ActiveContextSnapshot activeSnapshot = contextSnapshot.activate();
            try {
                return delegate.call();
            } finally {
                activeSnapshot.deactivate();
            }
        }
    }

    private static class ConPropExecutorService implements ExecutorService {

        protected final ContextProvider contextProvider;
        private final ExecutorService delegate;

        ConPropExecutorService(ContextProvider contextProvider, ExecutorService delegate) {
            this.contextProvider = contextProvider;
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.submit(new ConPropCallable<>(contextSnapshot, task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.submit(new ConPropRunnable(contextSnapshot, task), result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.submit(new ConPropRunnable(contextSnapshot, task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            List<Callable<T>> conPropTasks = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                conPropTasks.add(new ConPropCallable<>(contextSnapshot, task));
            }
            return delegate.invokeAll(conPropTasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            List<Callable<T>> conPropTasks = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                conPropTasks.add(new ConPropCallable<>(contextSnapshot, task));
            }
            return delegate.invokeAll(conPropTasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            List<Callable<T>> conPropTasks = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                conPropTasks.add(new ConPropCallable<>(contextSnapshot, task));
            }
            return delegate.invokeAny(conPropTasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            List<Callable<T>> conPropTasks = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                conPropTasks.add(new ConPropCallable<>(contextSnapshot, task));
            }
            return delegate.invokeAny(conPropTasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            delegate.execute(new ConPropRunnable(contextSnapshot, command));
        }
    }

    private static class ConPropScheduledExecutorService extends ConPropExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService delegate;

        ConPropScheduledExecutorService(ContextProvider contextProvider, ScheduledExecutorService delegate) {
            super(contextProvider, delegate);
            this.delegate = delegate;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.schedule(new ConPropRunnable(contextSnapshot, command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.schedule(new ConPropCallable<>(contextSnapshot, callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.scheduleAtFixedRate(new ConPropRunnable(contextSnapshot, command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            ContextSnapshot contextSnapshot = contextProvider.capture();
            return delegate.scheduleWithFixedDelay(new ConPropRunnable(contextSnapshot, command), initialDelay, delay, unit);
        }
    }
}
