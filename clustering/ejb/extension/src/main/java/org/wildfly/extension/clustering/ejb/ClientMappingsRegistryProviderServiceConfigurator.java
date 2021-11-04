package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.ClientMappingsRegistryProvider;
import org.wildfly.clustering.service.FunctionalService;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class service configurator for client mappings registry providers.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public abstract class ClientMappingsRegistryProviderServiceConfigurator extends CapabilityServiceNameProvider
        implements ResourceServiceConfigurator, Supplier<ClientMappingsRegistryProvider> {

    public ClientMappingsRegistryProviderServiceConfigurator(PathAddress address) {
        super(ClientMappingsRegistryProviderResourceDefinition.Capability.CLIENT_MAPPINGS_REGISTRY_PROVIDER, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<ClientMappingsRegistryProvider> provider = builder.provides(name);
        Service service = new FunctionalService<>(provider, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
