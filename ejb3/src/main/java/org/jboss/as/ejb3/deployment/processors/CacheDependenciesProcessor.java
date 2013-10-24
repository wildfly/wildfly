package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderRegistry;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderRegistryService;
import org.jboss.as.ejb3.component.stateful.VersionedMarshallingConfigurationService;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.util.Collection;

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

        final ServiceTarget target = context.getServiceTarget();
        @SuppressWarnings("rawtypes")
        final InjectedValue<CacheFactoryBuilderRegistry> registry = new InjectedValue<>();
        Service<Void> service = new AbstractService<Void>() {
            @Override
            public void start(StartContext context) {
                // Install dependencies for each registered cache factory builder
                Collection<CacheFactoryBuilder<?, ?>> builders = registry.getValue().getBuilders();
                for (CacheFactoryBuilder<?, ?> builder: builders) {
                    builder.installDeploymentUnitDependencies(target, name);
                }
            }
        };
        target.addService(name.append("cache-dependencies-installer"), service)
                .addDependency(CacheFactoryBuilderRegistryService.SERVICE_NAME, CacheFactoryBuilderRegistry.class, registry)
                .install()
        ;

        // Install versioned marshalling configuration
        InjectedValue<ModuleDeployment> deployment = new InjectedValue<>();
        Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        Value<ModuleLoader> moduleLoader = new ImmediateValue<>(module.getModuleLoader());
        target.addService(VersionedMarshallingConfigurationService.getServiceName(name), new VersionedMarshallingConfigurationService(deployment, moduleLoader))
                .addDependency(name.append(ModuleDeployment.SERVICE_NAME), ModuleDeployment.class, deployment)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing
    }
}
