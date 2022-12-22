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

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.DeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupManager;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupManagerServiceNameProvider;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanGroupManager;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanGroupManagerConfiguration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that provides a {@link BeanGroupManager} for a bean deployment.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class InfinispanBeanGroupManagerServiceConfigurator<K, V extends BeanInstance<K>> extends BeanGroupManagerServiceNameProvider implements CapabilityServiceConfigurator, Supplier<BeanGroupManager<K, V>>, InfinispanConfiguration {

    private final SupplierDependency<Cache<?, ?>> cache;
    private final SupplierDependency<ByteBufferMarshaller> marshaller;

    public InfinispanBeanGroupManagerServiceConfigurator(DeploymentConfiguration config, SupplierDependency<Cache<?, ?>> cache, SupplierDependency<ByteBufferMarshaller> marshaller) {
        super(config);
        this.cache = cache;
        this.marshaller = marshaller;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<BeanGroupManager<K, V>> manager = new CompositeDependency(this.marshaller, this.cache).register(builder).provides(name);
        return builder.setInstance(new FunctionalService<>(manager, Function.identity(), this));
    }

    @Override
    public BeanGroupManager<K, V> get() {
        CacheProperties properties = this.getCacheProperties();
        InfinispanBeanGroupManager<K, V, ByteBufferMarshaller> factory = new InfinispanBeanGroupManager<>(this);
        MarshalledValueFactory<ByteBufferMarshaller> marshalledValueFactory = new ByteBufferMarshalledValueFactory(this.marshaller.get());
        return new DefaultBeanGroupManager<>(new DefaultBeanGroupManagerConfiguration<K, V, ByteBufferMarshaller>() {
            @Override
            public Creator<K, MarshalledValue<Map<K, V>, ByteBufferMarshaller>, MarshalledValue<Map<K, V>, ByteBufferMarshaller>> getCreator() {
                return factory;
            }

            @Override
            public Remover<K> getRemover() {
                return factory;
            }

            @Override
            public MutatorFactory<K, MarshalledValue<Map<K, V>, ByteBufferMarshaller>> getMutatorFactory() {
                return factory;
            }

            @Override
            public CacheProperties getCacheProperties() {
                return properties;
            }

            @Override
            public MarshalledValueFactory<ByteBufferMarshaller> getMarshalledValueFactory() {
                return marshalledValueFactory;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <KK, VV> Cache<KK, VV> getCache() {
        return (Cache<KK, VV>) this.cache.get();
    }
}
