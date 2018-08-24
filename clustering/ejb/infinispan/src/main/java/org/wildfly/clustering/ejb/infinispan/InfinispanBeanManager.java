/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ee.retry.RetryingInvoker;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.PredicateCacheEventFilter;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
@Listener(primaryOnly = true)
public class InfinispanBeanManager<I, T> implements BeanManager<I, T, TransactionBatch> {

    private static ThreadFactory createThreadFactory() {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(InfinispanBeanManager.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return WildFlySecurityManager.doUnchecked(action);
    }

    private static final Invoker INVOKER = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(10), Duration.ofMillis(100));

    private final Cache<BeanKey<I>, BeanEntry<I>> cache;
    private final CacheProperties properties;
    private final BeanFactory<I, T> beanFactory;
    private final BeanGroupFactory<I, T> groupFactory;
    private final IdentifierFactory<I> identifierFactory;
    private final KeyAffinityService<BeanKey<I>> affinity;
    private final Registry<String, ?> registry;
    private final NodeFactory<Address> nodeFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final ExpirationConfiguration<T> expiration;
    private final PassivationConfiguration<T> passivation;
    private final Batcher<TransactionBatch> batcher;
    private final Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> filter;
    private final AtomicReference<Future<?>> rehashFuture = new AtomicReference<>();

    private volatile Scheduler<I> scheduler;
    private volatile ExecutorService executor;
    private volatile CommandDispatcher<Scheduler<I>> dispatcher;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<I, T> configuration, IdentifierFactory<I> identifierFactory, Configuration<BeanKey<I>, BeanEntry<I>, BeanFactory<I, T>> beanConfiguration, Configuration<BeanGroupKey<I>, BeanGroupEntry<I, T>, BeanGroupFactory<I, T>> groupConfiguration) {
        this.filter = configuration.getBeanFilter();
        this.groupFactory = groupConfiguration.getFactory();
        this.beanFactory = beanConfiguration.getFactory();
        this.cache = beanConfiguration.getCache();
        this.properties = configuration.getProperties();
        this.batcher = new InfinispanBatcher(this.cache);
        Address address = this.cache.getCacheManager().getAddress();
        KeyAffinityServiceFactory affinityFactory = configuration.getAffinityFactory();
        KeyGenerator<BeanKey<I>> beanKeyGenerator = () -> beanConfiguration.getFactory().createKey(identifierFactory.createIdentifier());
        this.affinity = affinityFactory.createService(this.cache, beanKeyGenerator);
        this.identifierFactory = () -> this.affinity.getKeyForAddress(address).getId();
        this.registry = configuration.getRegistry();
        this.nodeFactory = configuration.getNodeFactory();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpirationConfiguration();
        this.passivation = configuration.getPassivationConfiguration();
    }

    @Override
    public void start() {
        this.executor = Executors.newSingleThreadExecutor(createThreadFactory());
        this.affinity.start();
        Time timeout = this.expiration.getTimeout();
        this.scheduler = (timeout != null) && (timeout.getValue() >= 0) ? new BeanExpirationScheduler<>(this.batcher, new ExpiredBeanRemover<>(this.beanFactory), this.expiration) : new Scheduler<I>() {
            @Override
            public void schedule(I id) {
            }

            @Override
            public void cancel(I id) {
            }

            @Override
            public void cancel(Locality locality) {
            }

            @Override
            public void close() {
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.filter.toString(), this.scheduler);
        this.cache.addListener(this, new PredicateCacheEventFilter<>(this.filter), null);
        this.schedule(new SimpleLocality(false), new CacheLocality(this.cache));
    }

    @Override
    public void stop() {
        this.groupFactory.close();
        this.cache.removeListener(this);
        PrivilegedAction<List<Runnable>> action = () -> this.executor.shutdownNow();
        WildFlySecurityManager.doUnchecked(action);
        try {
            this.executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.dispatcher.close();
            this.scheduler.close();
            this.affinity.stop();
        }
    }

    @Override
    public boolean isRemotable(Throwable throwable) {
        return !(throwable instanceof CacheException);
    }

    @Override
    public Affinity getStrictAffinity() {
        Group group = this.registry.getGroup();
        return this.cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(this.registry.getEntry(group.getLocalMember()).getKey());
    }

    @Override
    public Affinity getWeakAffinity(I id) {
        if (this.cache.getCacheConfiguration().clustering().cacheMode().isClustered()) {
            Node node = this.locatePrimaryOwner(id);
            Map.Entry<String, ?> entry = this.registry.getEntry(node);
            if (entry != null) {
                return new NodeAffinity(entry.getKey());
            }
        }
        return Affinity.NONE;
    }

    private void cancel(Bean<I, T> bean) {
        try {
            this.executeOnPrimaryOwner(bean, new CancelSchedulerCommand<>(bean));
        } catch (Exception e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToCancelBean(e, bean.getId());
        }
    }

    void schedule(Bean<I, T> bean) {
        try {
            this.executeOnPrimaryOwner(bean, new ScheduleSchedulerCommand<>(bean));
        } catch (Exception e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToScheduleBean(e, bean.getId());
        }
    }

    private void executeOnPrimaryOwner(Bean<I, T> bean, final Command<Void, Scheduler<I>> command) throws CommandDispatcherException {
        CommandDispatcher<Scheduler<I>> dispatcher = this.dispatcher;
        ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException> action = new ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException>() {
            @Override
            public CompletionStage<Void> get() throws CommandDispatcherException {
                // This should only go remote following a failover
                Node node = InfinispanBeanManager.this.locatePrimaryOwner(bean.getId());
                return dispatcher.executeOnMember(command, node);
            }
        };
        INVOKER.invoke(action).toCompletableFuture().join();
    }

    Node locatePrimaryOwner(I id) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) && !this.cache.getCacheConfiguration().clustering().cacheMode().isScattered() ? dist.getCacheTopology().getDistribution(new Key<>(id)).primary() : null;
        Node member = (address != null) ? this.nodeFactory.createNode(address) : null;
        return (member != null) ? member : this.registry.getGroup().getLocalMember();
    }

    @Override
    public Bean<I, T> createBean(I id, I groupId, T bean) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, this.groupFactory.createValue(groupId, null));
        group.addBean(id, bean);
        group.releaseBean(id, this.properties.isPersistent() ? this.passivation.getPassivationListener() : null);
        return new SchedulableBean(this.beanFactory.createBean(id, this.beanFactory.createValue(id, groupId)));
    }

    @Override
    public Bean<I, T> findBean(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Locating bean %s", id);
        BeanEntry<I> entry = this.beanFactory.findValue(id);
        Bean<I, T> bean = (entry != null) ? this.beanFactory.createBean(id, entry) : null;
        if (bean == null) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        this.cancel(bean);
        return new SchedulableBean(bean);
    }

    @Override
    public boolean containsBean(I id) {
        return this.cache.containsKey(this.beanFactory.createKey(id));
    }

    @Override
    public IdentifierFactory<I> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public int getActiveCount() {
        try (Stream<Map.Entry<BeanKey<I>, BeanEntry<I>>> entries = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).entrySet().stream()) {
            return (int) entries.filter(this.filter).count();
        }
    }

    @Override
    public int getPassiveCount() {
        return this.groupFactory.getPassiveCount();
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<BeanKey<I>, BeanEntry<I>> event) {
        Locality newLocality = new ConsistentHashLocality(event.getCache(), event.getConsistentHashAtEnd());
        if (event.isPre()) {
            Future<?> future = this.rehashFuture.getAndSet(null);
            if (future != null) {
                future.cancel(true);
            }
            try {
                this.executor.submit(() -> {
                    this.scheduler.cancel(newLocality);
                });
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        } else {
            Locality oldLocality = new ConsistentHashLocality(event.getCache(), event.getConsistentHashAtStart());
            try {
                this.rehashFuture.set(this.executor.submit(() -> this.schedule(oldLocality, newLocality)));
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
    }

    private void schedule(Locality oldLocality, Locality newLocality) {
        // Iterate over beans in memory
        try (Stream<Map.Entry<BeanKey<I>, BeanEntry<I>>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).entrySet().stream().filter(this.filter)) {
            Iterator<Map.Entry<BeanKey<I>, BeanEntry<I>>> entries = stream.iterator();
            while (entries.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                Map.Entry<BeanKey<I>, BeanEntry<I>> entry = entries.next();
                BeanKey<I> key = entry.getKey();
                // If we are the new primary owner of this bean then schedule expiration of this bean locally
                if (this.filter.test(entry) && !oldLocality.isLocal(key) && newLocality.isLocal(key)) {
                    this.scheduler.schedule(key.getId());
                }
            }
        }
    }

    private class SchedulableBean implements Bean<I, T> {

        private final Bean<I, T> bean;

        SchedulableBean(Bean<I, T> bean) {
            this.bean = bean;
        }

        @Override
        public I getId() {
            return this.bean.getId();
        }

        @Override
        public I getGroupId() {
            return this.bean.getGroupId();
        }

        @Override
        public void remove(RemoveListener<T> listener) {
            this.bean.remove(listener);
        }

        @Override
        public boolean isExpired() {
            return this.bean.isExpired();
        }

        @Override
        public boolean isValid() {
            return this.bean.isValid();
        }

        @Override
        public T acquire() {
            return this.bean.acquire();
        }

        @Override
        public boolean release() {
            return this.bean.release();
        }

        @Override
        public void close() {
            this.bean.close();
            if (this.bean.isValid()) {
                InfinispanBeanManager.this.schedule(this.bean);
            }
        }
    }
}
