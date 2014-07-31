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
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;

/**
 * Service that provides a distributable {@link CacheFactory}.
 *
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryService<K, V extends Identifiable<K> & Contextual<Batch>> extends AbstractService<CacheFactory<K, V>> implements CacheFactory<K, V> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Identifiable<K> & Contextual<Batch>> ServiceBuilder<CacheFactory<K, V>> build(ServiceTarget target, ServiceName name, ServiceName factoryServiceName) {
        DistributableCacheFactoryService<K, V> service = new DistributableCacheFactoryService<>();
        return target.addService(name, service)
                .addDependency(factoryServiceName, BeanManagerFactory.class, (Injector) service.factory)
        ;
    }

    private final InjectedValue<BeanManagerFactory<UUID, K, V, Batch>> factory = new InjectedValue<>();

    private DistributableCacheFactoryService() {
        // Hide
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
