/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.ejb.cache.timer.RemappableTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerManagerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagerFactory<I> implements TimerManagerFactory<I> {

    private final InfinispanTimerManagerFactoryConfiguration<I> configuration;

    public InfinispanTimerManagerFactory(InfinispanTimerManagerFactoryConfiguration<I> configuration) {
        this.configuration = configuration;
    }

    @Override
    public TimerManager<I> createTimerManager(TimerManagerConfiguration<I> configuration) {
        InfinispanTimerManagerFactoryConfiguration<I> factoryConfiguration = this.configuration;
        Marshaller<Object, MarshalledValue<Object, ByteBufferMarshaller>> marshaller = new MarshalledValueMarshaller<>(new ByteBufferMarshalledValueFactory(this.configuration.getMarshaller()));

        InfinispanTimerMetaDataConfiguration<MarshalledValue<Object, ByteBufferMarshaller>> metaDataFactoryConfig = new InfinispanTimerMetaDataConfiguration<>() {
            @Override
            public Marshaller<Object, MarshalledValue<Object, ByteBufferMarshaller>> getMarshaller() {
                return marshaller;
            }

            @Override
            public boolean isPersistent() {
                return configuration.isPersistent();
            }

            @Override
            public <K, V> Cache<K, V> getCache() {
                return factoryConfiguration.getCache();
            }
        };
        TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<MarshalledValue<Object, ByteBufferMarshaller>>> metaDataFactory = new InfinispanTimerMetaDataFactory<>(metaDataFactoryConfig);
        TimerFactory<I, RemappableTimerMetaDataEntry<MarshalledValue<Object, ByteBufferMarshaller>>> factory = new InfinispanTimerFactory<>(metaDataFactory, configuration.getListener());

        return new InfinispanTimerManager<>(new InfinispanTimerManagerConfiguration<I, MarshalledValue<Object, ByteBufferMarshaller>>() {
            @Override
            public <K, V> Cache<K, V> getCache() {
                return factoryConfiguration.getCache();
            }

            @Override
            public CacheProperties getCacheProperties() {
                return factoryConfiguration.getCacheProperties();
            }

            @Override
            public TimerFactory<I, RemappableTimerMetaDataEntry<MarshalledValue<Object, ByteBufferMarshaller>>> getTimerFactory() {
                return factory;
            }

            @Override
            public TimerRegistry<I> getRegistry() {
                return factoryConfiguration.getRegistry();
            }

            @Override
            public Marshaller<Object, MarshalledValue<Object, ByteBufferMarshaller>> getMarshaller() {
                return marshaller;
            }

            @Override
            public Supplier<Batch> getBatchFactory() {
                return factoryConfiguration.getBatchFactory();
            }

            @Override
            public Supplier<I> getIdentifierFactory() {
                return factoryConfiguration.getIdentifierFactory();
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return factoryConfiguration.getCommandDispatcherFactory();
            }
        });
    }
}
