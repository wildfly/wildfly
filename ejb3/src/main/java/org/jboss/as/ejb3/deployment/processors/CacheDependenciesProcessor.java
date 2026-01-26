/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class CacheDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        final CapabilityServiceSupport support = unit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);

        Set<ServiceDependency<StatefulSessionBeanCacheProvider>> dependencies = new HashSet<>();
        for (ComponentDescription description : moduleDescription.getComponentDescriptions()) {
            if (description instanceof StatefulComponentDescription) {
                StatefulComponentDescription statefulDescription = (StatefulComponentDescription) description;
                dependencies.add(getStatefulSessionBeanCacheProviderDependency(statefulDescription));
            }
        }

        // WFLY-21390 Compute bean classes before the lambda to avoid capturing EEModuleConfiguration
        EEModuleConfiguration moduleConfiguration = unit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        Set<Class<?>> beanClasses = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            if (configuration.getComponentDescription() instanceof StatefulComponentDescription) {
                Class<?> componentClass = configuration.getComponentClass();
                while (componentClass != Object.class) {
                    beanClasses.add(componentClass);
                    componentClass = componentClass.getSuperclass();
                }
            }
        }

        ServiceInstaller installer = new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                // Cache provider dependencies might still contain duplicates (if referenced via alias), so ensure we collect only distinct instances.
                Set<StatefulSessionBeanCacheProvider> providers = dependencies.stream().map(Supplier::get).distinct().collect(Collectors.toSet());
                for (StatefulSessionBeanCacheProvider provider : providers) {
                    for (ServiceInstaller deploymentInstaller : provider.getDeploymentServiceInstallers(unit, beanClasses)) {
                        deploymentInstaller.install(target);
                    }
                }
                return null;
            }
        };
        ServiceInstaller.builder(installer, support).requires(dependencies).build().install(context);
    }

    private static ServiceDependency<StatefulSessionBeanCacheProvider> getStatefulSessionBeanCacheProviderDependency(StatefulComponentDescription description) {
        if (!description.isPassivationApplicable()) return ServiceDependency.on(StatefulSessionBeanCacheProvider.PASSIVATION_DISABLED_SERVICE_DESCRIPTOR);
        CacheInfo cache = description.getCache();
        return (cache != null) ? ServiceDependency.on(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, cache.getName()) : ServiceDependency.on(StatefulSessionBeanCacheProvider.DEFAULT_SERVICE_DESCRIPTOR);
    }
}
