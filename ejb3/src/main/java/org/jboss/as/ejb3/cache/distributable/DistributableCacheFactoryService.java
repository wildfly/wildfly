/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.cache.distributable;

import java.util.UUID;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.service.Builder;

/**
 * Service that provides a distributable {@link CacheFactory}.
 *
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryService<K, V extends Identifiable<K> & Contextual<Batch>> implements Builder<CacheFactory<K, V>>, Value<CacheFactory<K, V>>, CacheFactory<K, V> {

    private final ServiceName name;
    private final Builder<? extends BeanManagerFactory<UUID, K, V, Batch>> builder;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<BeanManagerFactory> factory = new InjectedValue<>();

    public DistributableCacheFactoryService(ServiceName name, Builder<? extends BeanManagerFactory<UUID, K, V, Batch>> builder) {
        this.name = name;
        this.builder = builder;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<CacheFactory<K, V>> build(ServiceTarget target) {
        this.builder.build(target).install();
        return target.addService(this.name, new ValueService<>(this))
                .addDependency(this.builder.getServiceName(), BeanManagerFactory.class, this.factory)
        ;
    }

    @Override
    public CacheFactory<K, V> getValue() {
        return this;
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        BeanManager<UUID, K, V, Batch> manager = this.factory.getValue().createBeanManager(new GroupIdentifierFactory(), identifierFactory, passivationListener, new RemoveListenerAdapter<>(factory));
        return new DistributableCache<>(manager, factory);
    }
}
