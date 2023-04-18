/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Manager;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
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

    private final Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> creator;
    private final MutatorFactory<K, MarshalledValue<Map<K, V>, C>> mutatorFactory;
    private final MarshalledValueFactory<C> factory;
    private final Manager<K, MutableBeanGroup<K, V>> manager;
    private final Consumer<Map<K, V>> postActivateTask;
    private final Consumer<Map<K, V>> prePassivateTask;

    public DefaultBeanGroupManager(DefaultBeanGroupManagerConfiguration<K, V, C> configuration) {
        this.creator = configuration.getCreator();
        this.mutatorFactory = configuration.getMutatorFactory();
        this.factory = configuration.getMarshalledValueFactory();
        boolean persistent = configuration.getCacheProperties().isPersistent();
        this.postActivateTask = persistent ? new MapValuesTask<>(BeanInstance::postActivate) : Functions.discardingConsumer();
        this.prePassivateTask = persistent ? new MapValuesTask<>(BeanInstance::prePassivate) : Functions.discardingConsumer();
        this.manager = new ConcurrentManager<>(Functions.discardingConsumer(), new NewBeanGroupCloseTask<>(configuration.getRemover()));
    }

    @Override
    public BeanGroup<K, V> getBeanGroup(K id) {
        Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> creator = this.creator;
        MutatorFactory<K, MarshalledValue<Map<K, V>, C>> mutatorFactory = this.mutatorFactory;
        MarshalledValueFactory<C> factory = this.factory;
        Consumer<Map<K, V>> postActivateTask = this.postActivateTask;
        Consumer<Map<K, V>> prePassivateTask = this.prePassivateTask;
        Function<Runnable, MutableBeanGroup<K, V>> beanGroupFactory = new Function<>() {
            @Override
            public MutableBeanGroup<K, V> apply(Runnable closeTask) {
                Map<K, V> instances = new ConcurrentHashMap<>();
                MarshalledValue<Map<K, V>, C> newValue = factory.createMarshalledValue(instances);
                MarshalledValue<Map<K, V>, C> value = creator.createValue(id, newValue);
                if (value != newValue) {
                    try {
                        instances = value.get(factory.getMarshallingContext());
                        postActivateTask.accept(instances);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
                Mutator mutator = mutatorFactory.createMutator(id, value);
                return new DefaultBeanGroup<>(id, instances, prePassivateTask, mutator, closeTask);
            }
        };
        return this.manager.apply(id, beanGroupFactory);
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
        private final Remover<K> remover;

        NewBeanGroupCloseTask(Remover<K> remover) {
            this.remover = remover;
        }

        @Override
        public void accept(MutableBeanGroup<K, V> group) {
            if (group.isEmpty()) {
                this.remover.remove(group.getId());
            } else {
                group.mutate();
            }
        }
    }
}
