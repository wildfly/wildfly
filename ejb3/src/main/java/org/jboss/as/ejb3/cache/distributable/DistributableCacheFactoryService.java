/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.cache.distributable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Service that provides a distributable {@link CacheFactory}.
 *
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryService<K, V extends Identifiable<K> & Contextual<Batch>> extends SimpleServiceNameProvider implements ServiceConfigurator, CacheFactory<K, V> {

    private final ServiceConfigurator configurator;
    private volatile Supplier<BeanManagerFactory<K, V, Batch>> factory;
    private volatile Supplier<TransactionSynchronizationRegistry> tsr;

    public DistributableCacheFactoryService(ServiceName name, ServiceConfigurator configurator) {
        super(name);
        this.configurator = configurator;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        this.factory = builder.requires(this.configurator.getServiceName());
        this.tsr = builder.requires(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY);
        Consumer<CacheFactory<K, V>> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, this);
        return builder.setInstance(service);
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        BeanManager<K, V, Batch> manager = this.factory.get().createBeanManager(identifierFactory, passivationListener, new RemoveListenerAdapter<>(factory));
        return new DistributableCache<>(manager, factory, this.tsr.get());
    }
}
