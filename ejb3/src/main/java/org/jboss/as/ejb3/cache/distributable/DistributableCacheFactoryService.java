/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.cache.distributable;

import java.util.UUID;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.Value;
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
public class DistributableCacheFactoryService<K, V extends Identifiable<K>> extends AbstractService<CacheFactory<K, V>> implements CacheFactory<K, V> {

    private final Value<BeanManagerFactory<UUID, K, V>> factory;
    private final Value<ServerEnvironment> environment;

    public DistributableCacheFactoryService(Value<BeanManagerFactory<UUID, K, V>> factory, Value<ServerEnvironment> environment) {
        this.factory = factory;
        this.environment = environment;
    }

    @Override
    public CacheFactory<K, V> getValue() {
        return this;
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        BeanManager<UUID, K, V> manager = this.factory.getValue().createBeanManager(new GroupIdentifierFactory(), identifierFactory, passivationListener, new RemoveListenerAdapter<>(factory));
        return new DistributableCache<>(manager, factory, this.environment.getValue());
    }
}
