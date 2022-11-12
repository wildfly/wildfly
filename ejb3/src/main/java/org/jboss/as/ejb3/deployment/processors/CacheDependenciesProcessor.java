package org.jboss.as.ejb3.deployment.processors;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProviderServiceNameProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class CacheDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        final ServiceName name = unit.getServiceName();
        EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        final CapabilityServiceSupport support = unit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        final ServiceTarget target = context.getServiceTarget();

        Set<SupplierDependency<StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance>>> dependencies = new HashSet<>();
        for (ComponentDescription description : moduleDescription.getComponentDescriptions()) {
            if (description instanceof StatefulComponentDescription) {
                StatefulComponentDescription statefulDescription = (StatefulComponentDescription) description;
                dependencies.add(new ServiceSupplierDependency<>(getCacheFactoryBuilderServiceName(statefulDescription)));
            }
        }

        EEModuleConfiguration moduleConfiguration = unit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);

        Service service = new ChildTargetService(new Consumer<ServiceTarget>() {
            @Override
            public void accept(ServiceTarget target) {
                // Cache factory builder dependencies might still contain duplicates (if referenced via alias), so ensure we collect only distinct instances.
                Set<StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance>> providers = new HashSet<>(dependencies.size());
                for (Supplier<StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance>> dependency : dependencies) {
                    providers.add(dependency.get());
                }
                for (StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance> provider : providers) {
                    for (CapabilityServiceConfigurator configurator : provider.getDeploymentServiceConfigurators(unit, moduleConfiguration)) {
                        configurator.configure(support).build(target).install();
                    }
                }
            }
        });

        ServiceBuilder<?> builder = target.addService(name.append("cache-dependencies-installer"));
        for (Dependency dependency : dependencies) {
            dependency.register(builder);
        }
        builder.setInstance(service).install();
    }

    private static ServiceName getCacheFactoryBuilderServiceName(StatefulComponentDescription description) {
        if (!description.isPassivationApplicable()) return StatefulSessionBeanCacheProviderServiceNameProvider.DEFAULT_PASSIVATION_DISABLED_CACHE_SERVICE_NAME;
        CacheInfo cache = description.getCache();
        return (cache != null) ? new StatefulSessionBeanCacheProviderServiceNameProvider(cache.getName()).getServiceName() : StatefulSessionBeanCacheProviderServiceNameProvider.DEFAULT_CACHE_SERVICE_NAME;
    }
}
