package org.jboss.as.ejb3.deployment.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.stateful.MarshallingConfigurationRepositoryValue;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

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
        List<ValueDependency<CacheFactoryBuilder>> list = new ArrayList<>();
        Set<InjectedValueDependency<CacheFactoryBuilder>> uniqueValues = new HashSet<>();
        for (ComponentDescription description : moduleDescription.getComponentDescriptions()) {
            if (description instanceof StatefulComponentDescription) {
                InjectedValueDependency<CacheFactoryBuilder> cacheFactoryBuilderInjectedValueDependency = new InjectedValueDependency<>(getCacheFactoryBuilderServiceName((StatefulComponentDescription) description), CacheFactoryBuilder.class);
                if (uniqueValues.add(cacheFactoryBuilderInjectedValueDependency)) {
                    list.add(cacheFactoryBuilderInjectedValueDependency);
                }
            }
        }
        @SuppressWarnings("rawtypes")
        Collection<ValueDependency<CacheFactoryBuilder>> cacheDependencies = list;
        Service<Void> service = new AbstractService<Void>() {
            @Override
            public void start(StartContext context) {
                // Install dependencies for each distinct cache factory builder referenced by the deployment
                cacheDependencies.stream()
                        .map(Value::getValue)
                        .distinct()
                        .forEach(builder -> builder.installDeploymentUnitDependencies(support, target, name));
            }
        };
        ServiceBuilder<Void> builder = target.addService(name.append("cache-dependencies-installer"), service);
        for (ValueDependency<CacheFactoryBuilder> dependency : cacheDependencies) {
            dependency.register(builder);
        }
        builder.install();

        // Install versioned marshalling configuration
        InjectedValue<ModuleDeployment> deployment = new InjectedValue<>();
        Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        target.addService(MarshallingConfigurationRepositoryValue.getServiceName(name), new ValueService<>(new MarshallingConfigurationRepositoryValue(deployment, new ImmediateValue<>(module))))
                .addDependency(name.append(ModuleDeployment.SERVICE_NAME), ModuleDeployment.class, deployment)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing
    }

    private static ServiceName getCacheFactoryBuilderServiceName(StatefulComponentDescription description) {
        if (!description.isPassivationApplicable()) return CacheFactoryBuilderService.DEFAULT_PASSIVATION_DISABLED_CACHE_SERVICE_NAME;
        CacheInfo cache = description.getCache();
        return (cache != null) ? CacheFactoryBuilderService.getServiceName(cache.getName()) : CacheFactoryBuilderService.DEFAULT_CACHE_SERVICE_NAME;
    }
}
