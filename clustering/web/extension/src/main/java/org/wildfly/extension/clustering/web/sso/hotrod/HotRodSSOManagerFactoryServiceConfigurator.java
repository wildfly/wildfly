/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.hotrod;

import java.util.function.Consumer;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.hotrod.sso.HotRodSSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOManagerFactoryServiceConfigurator<A, D, S> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, HotRodConfiguration {

    private final String name;
    private final HotRodSSOManagementConfiguration config;

    private final CapabilityServiceConfigurator configurator;
    private volatile SupplierDependency<RemoteCache<?, ?>> cache;

    public HotRodSSOManagerFactoryServiceConfigurator(HotRodSSOManagementConfiguration config, String name) {
        super(ServiceName.JBOSS.append("clustering", "sso", name));

        this.name = name;
        this.config = config;
        String configurationName = this.config.getConfigurationName();
        String templateName = (configurationName != null) ? configurationName : DefaultTemplate.DIST_SYNC.getTemplateName();
        this.configurator = new RemoteCacheServiceConfigurator<>(this.getServiceName().append("cache"), this.config.getContainerName(), this.name, new Consumer<RemoteCacheConfigurationBuilder>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).nearCacheMode(NearCacheMode.INVALIDATED).templateName(templateName).transactionMode(TransactionMode.NONE);
            }
        });
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.cache = new ServiceSupplierDependency<>(this.configurator.configure(support).getServiceName());
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SSOManagerFactory<A, D, S, TransactionBatch>> factory = this.cache.register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(factory, new HotRodSSOManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> RemoteCache<K, V> getCache() {
        return (RemoteCache<K, V>) this.cache.get();
    }
}
