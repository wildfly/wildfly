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

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
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
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.DeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanDeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service providing a distributable stateful session bean cache provider.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public abstract class AbstractDistributableStatefulSessionBeanCacheProviderServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends StatefulSessionBeanCacheProviderServiceNameProvider implements ResourceServiceConfigurator, StatefulSessionBeanCacheProvider<K, V>, Consumer<SupplierDependency<BeanManagementProvider>> {

    private volatile SupplierDependency<BeanManagementProvider> provider;

    public AbstractDistributableStatefulSessionBeanCacheProviderServiceConfigurator(PathAddress address) {
        super(address.getLastElement().getValue());
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<StatefulSessionBeanCacheProvider<K, V>> provider = this.provider.register(builder).provides(name);
        Service service = Service.newInstance(provider, this);
        return builder.setInstance(service);
    }

    @Override
    public void accept(SupplierDependency<BeanManagementProvider> provider) {
        this.provider = provider;
    }

    String getBeanManagerName(DeploymentUnit unit) {
        BeanManagementProvider provider = this.provider.get();
        List<String> parts = new ArrayList<>(3);
        DeploymentUnit parent = unit.getParent();
        if (parent != null) {
            parts.add(parent.getServiceName().getSimpleName());
        }
        parts.add(unit.getServiceName().getSimpleName());
        parts.add(provider.getName());
        return String.join("/", parts);
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit, EEModuleConfiguration moduleConfiguration) {
        return this.provider.get().getDeploymentServiceConfigurators(new BeanDeploymentUnitConfiguration(this.provider.get(), unit, moduleConfiguration));
    }

    @Override
    public CapabilityServiceConfigurator getStatefulBeanCacheFactoryServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration) {
        CapabilityServiceConfigurator configurator = this.provider.get().getBeanManagerFactoryServiceConfigurator(new DeploymentUnitBeanConfiguration(this.provider.get(), unit, configuration));
        return new DistributableStatefulSessionBeanCacheFactoryServiceConfigurator<>(description.getCacheFactoryServiceName(), configurator);
    }

    @Override
    public boolean supportsPassivation() {
        return true;
    }

    private static class DeploymentUnitConfiguration implements DeploymentConfiguration {

        private final String deploymentName;
        private final DeploymentUnit unit;

        DeploymentUnitConfiguration(BeanManagementProvider provider, DeploymentUnit unit) {
            List<String> parts = new ArrayList<>(3);
            DeploymentUnit parent = unit.getParent();
            if (parent != null) {
                parts.add(parent.getServiceName().getSimpleName());
            }
            parts.add(unit.getServiceName().getSimpleName());
            parts.add(provider.getName());
            this.deploymentName = String.join("/", parts);
            this.unit = unit;
        }

        @Override
        public String getDeploymentName() {
            return this.deploymentName;
        }

        @Override
        public ServiceName getDeploymentServiceName() {
            return this.unit.getServiceName();
        }

        @Override
        public Module getModule() {
            return this.unit.getAttachment(Attachments.MODULE);
        }
    }

    private static class BeanDeploymentUnitConfiguration extends DeploymentUnitConfiguration implements BeanDeploymentConfiguration {
        private final Set<Class<?>> beanClasses = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

        BeanDeploymentUnitConfiguration(BeanManagementProvider provider, DeploymentUnit unit, EEModuleConfiguration moduleConfiguration) {
            super(provider, unit);
            for (ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
                if (configuration.getComponentDescription() instanceof StatefulComponentDescription) {
                    Class<?> componentClass = configuration.getComponentClass();
                    while (componentClass != Object.class) {
                        this.beanClasses.add(componentClass);
                        componentClass = componentClass.getSuperclass();
                    }
                }
            }
        }

        @Override
        public Set<Class<?>> getBeanClasses() {
            return this.beanClasses;
        }
    }

    private static class DeploymentUnitBeanConfiguration extends DeploymentUnitConfiguration implements BeanConfiguration {

        private final ComponentConfiguration configuration;

        DeploymentUnitBeanConfiguration(BeanManagementProvider provider, DeploymentUnit unit, ComponentConfiguration configuration) {
            super(provider, unit);
            this.configuration = configuration;
        }

        @Override
        public String getName() {
            return this.configuration.getComponentName();
        }
    }
}
