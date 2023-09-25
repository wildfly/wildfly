/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.util.Collections;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProviderServiceNameProvider;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Configures a service that provides a simple stateful session bean cache provider.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBeanCacheProviderServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends StatefulSessionBeanCacheProviderServiceNameProvider implements ResourceServiceConfigurator, StatefulSessionBeanCacheProvider<K, V> {

    public SimpleStatefulSessionBeanCacheProviderServiceConfigurator(PathAddress address) {
        super(address.getLastElement().getValue());
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<StatefulSessionBeanCacheProvider<K, V>> provider = builder.provides(name);
        Service service = Service.newInstance(provider, this);
        return builder.setInstance(service);
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit, EEModuleConfiguration moduleConfiguration) {
        return Collections.emptyList();
    }

    @Override
    public CapabilityServiceConfigurator getStatefulBeanCacheFactoryServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration) {
        return new SimpleStatefulSessionBeanCacheFactoryServiceConfigurator<>(description);
    }

    @Override
    public boolean supportsPassivation() {
        return false;
    }
}
