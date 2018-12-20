/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.cache.distributable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
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
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service that provides a distributable {@link CacheFactory}.
 *
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryServiceConfigurator<K, V extends Identifiable<K> & Contextual<Batch>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, CacheFactory<K, V> {

    private final CapabilityServiceConfigurator configurator;
    private final SupplierDependency<BeanManagerFactory<K, V, Batch>> factory;

    private volatile Supplier<TransactionSynchronizationRegistry> tsr;

    public DistributableCacheFactoryServiceConfigurator(ServiceName name, CapabilityServiceConfigurator configurator) {
        super(name);
        this.configurator = configurator;
        this.factory = new ServiceSupplierDependency<>(configurator);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        // Ensure the local transaction synchronization registry is started before the cache
        // This parsing isn't 100% ideal as it's somewhat 'internal' knowledge of the relationship between
        // capability names and service names. But at this point that relationship really needs to become
        // a contract anyway
        ServiceName tsrServiceName = ServiceNameFactory.parseServiceName("org.wildfly.transactions.transaction-synchronization-registry");
        this.tsr = builder.requires(tsrServiceName);
        Consumer<CacheFactory<K, V>> factory = this.factory.register(builder).provides(name);
        Service service = Service.newInstance(factory, this);
        return builder.setInstance(service);
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        BeanManager<K, V, Batch> manager = this.factory.get().createBeanManager(identifierFactory, passivationListener, new RemoveListenerAdapter<>(factory));
        return new DistributableCache<>(manager, factory, this.tsr.get());
    }
}
