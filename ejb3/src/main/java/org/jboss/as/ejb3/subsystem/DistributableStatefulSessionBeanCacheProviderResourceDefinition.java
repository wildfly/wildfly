/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.component.stateful.cache.distributable.DistributableStatefulSessionBeanCacheFactoryServiceInstallerFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.DeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanDeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are non distributed and do not have passivation-enabled.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableStatefulSessionBeanCacheProviderResourceDefinition extends StatefulSessionBeanCacheProviderResourceDefinition implements Function<String, ServiceDependency<StatefulSessionBeanCacheProvider>> {

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        BEAN_MANAGEMENT(EJB3SubsystemModel.BEAN_MANAGEMENT, ModelType.STRING, CapabilityReferenceRecorder.builder(CAPABILITY, BeanManagementProvider.SERVICE_DESCRIPTOR).build())
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder<?> referenece) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setCapabilityReference(referenece)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public DistributableStatefulSessionBeanCacheProviderResourceDefinition() {
        super(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public ServiceDependency<StatefulSessionBeanCacheProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return this.apply(Attribute.BEAN_MANAGEMENT.resolveModelAttribute(context, model).asStringOrNull());
    }

    @Override
    public ServiceDependency<StatefulSessionBeanCacheProvider> apply(String name) {
        return ServiceDependency.on(BeanManagementProvider.SERVICE_DESCRIPTOR, name).map(new Function<>() {
            @Override
            public StatefulSessionBeanCacheProvider apply(BeanManagementProvider provider) {
                return new StatefulSessionBeanCacheProvider() {
                    @Override
                    public boolean supportsPassivation() {
                        return true;
                    }

                    @Override
                    public Iterable<ServiceInstaller> getStatefulBeanCacheFactoryServiceInstallers(DeploymentUnit unit, StatefulComponentDescription description, String componentName) {
                        ServiceName name = unit.getServiceName().append(componentName).append("bean-manager");
                        ServiceInstaller beanManagerFactoryInstaller = provider.getBeanManagerFactoryServiceInstaller(name, new DeploymentUnitBeanConfiguration(provider, unit, componentName));
                        ServiceInstaller cacheFactoryInstaller = new DistributableStatefulSessionBeanCacheFactoryServiceInstallerFactory<>().apply(description, ServiceDependency.on(name));
                        return List.of(beanManagerFactoryInstaller, cacheFactoryInstaller);
                    }

                    @Override
                    public Iterable<ServiceInstaller> getDeploymentServiceInstallers(DeploymentUnit unit, Set<Class<?>> beanClasses) {
                        return provider.getDeploymentServiceInstallers(new BeanDeploymentUnitConfiguration(provider, unit, beanClasses));
                    }
                };
            }
        });
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
        private final Set<Class<?>> beanClasses;

        BeanDeploymentUnitConfiguration(BeanManagementProvider provider, DeploymentUnit unit, Set<Class<?>> beanClasses) {
            super(provider, unit);
            this.beanClasses = beanClasses;
        }

        @Override
        public Set<Class<?>> getBeanClasses() {
            return this.beanClasses;
        }
    }

    private static class DeploymentUnitBeanConfiguration extends DeploymentUnitConfiguration implements BeanConfiguration {

        private final String componentName;

        DeploymentUnitBeanConfiguration(BeanManagementProvider provider, DeploymentUnit unit, String componentName) {
            super(provider, unit);
            this.componentName = componentName;
        }

        @Override
        public String getName() {
            return this.componentName;
        }
    }
}
