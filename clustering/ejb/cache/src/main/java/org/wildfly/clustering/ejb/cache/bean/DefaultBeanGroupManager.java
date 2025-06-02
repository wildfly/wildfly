/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.wildfly.clustering.cache.CacheEntryCreator;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.cache.CacheEntryMutatorFactory;
import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.server.cache.Cache;
import org.wildfly.clustering.server.cache.CacheStrategy;
import org.wildfly.clustering.server.manager.Manager;
import org.wildfly.common.function.Functions;

/**
 * A manager for bean groups that leverages a {@link Manager} to handle bean group lifecycle.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public class DefaultBeanGroupManager<K, V extends BeanInstance<K>, C> implements BeanGroupManager<K, V> {
    static final Logger LOGGER = Logger.getLogger(DefaultBeanGroupManager.class);

    private final CacheEntryCreator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> creator;
    private final CacheEntryMutatorFactory<K, MarshalledValue<Map<K, V>, C>> mutatorFactory;
    private final MarshalledValueFactory<C> factory;
    private final Cache<K, MutableBeanGroup<K, V>> cache;
    private final Consumer<Map<K, V>> postActivateTask;
    private final Consumer<Map<K, V>> prePassivateTask;
    private final BiFunction<K, Runnable, MutableBeanGroup<K, V>> beanGroupFactory;

    public DefaultBeanGroupManager(DefaultBeanGroupManagerConfiguration<K, V, C> configuration) {
        this.creator = configuration.getCreator();
        this.mutatorFactory = configuration.getMutatorFactory();
        this.factory = configuration.getMarshalledValueFactory();
        boolean persistent = configuration.getCacheProperties().isPersistent();
        this.postActivateTask = persistent ? new MapValuesTask<>(BeanInstance::postActivate) : Functions.discardingConsumer();
        this.prePassivateTask = persistent ? new MapValuesTask<>(BeanInstance::prePassivate) : Functions.discardingConsumer();
        this.cache = CacheStrategy.CONCURRENT.createCache(Functions.discardingConsumer(), new NewBeanGroupCloseTask<>(configuration.getRemover()));
        this.beanGroupFactory = (id, closeTask) -> {
            Map<K, V> instances = new ConcurrentHashMap<>();
            MarshalledValue<Map<K, V>, C> newValue = this.factory.createMarshalledValue(instances);
            MarshalledValue<Map<K, V>, C> value = this.creator.createValue(id, newValue);
            if (value != newValue) {
                try {
                    instances = value.get(this.factory.getMarshallingContext());
                    this.postActivateTask.accept(instances);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            CacheEntryMutator mutator = this.mutatorFactory.createMutator(id, value);
            return new DefaultBeanGroup<>(id, instances, this.prePassivateTask, mutator, closeTask);
        };
    }

    @Override
    public BeanGroup<K, V> getBeanGroup(K id) {
        return this.cache.computeIfAbsent(id, this.beanGroupFactory);
    }

    private static class MapValuesTask<K, V> implements Consumer<Map<K, V>> {
        private final Consumer<V> task;

        MapValuesTask(Consumer<V> task) {
            this.task = task;
        }

        @Override
        public void accept(Map<K, V> instances) {
            instances.values().forEach(this.task);
        }
    }

    private static class NewBeanGroupCloseTask<K, V extends BeanInstance<K>, C> implements Consumer<MutableBeanGroup<K, V>> {
        private final CacheEntryRemover<K> remover;

        NewBeanGroupCloseTask(CacheEntryRemover<K> remover) {
            this.remover = remover;
        }

        @Override
        public void accept(MutableBeanGroup<K, V> group) {
            if (group.isEmpty()) {
                this.remover.remove(group.getId());
            } else {
                group.run();
            }
        }
    }
}
