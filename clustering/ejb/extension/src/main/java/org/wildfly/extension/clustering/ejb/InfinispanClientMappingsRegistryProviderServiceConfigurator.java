package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.InfinispanClientMappingsRegistryProviderResourceDefinition.Attribute.CACHE_CONTAINER;
import static org.wildfly.extension.clustering.ejb.InfinispanClientMappingsRegistryProviderResourceDefinition.Attribute.CACHE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.infinispan.InfinispanClientMappingsRegistryProvider;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Service configurator for Infinispan-based distributed client mappings registry provider.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProviderServiceConfigurator extends ClientMappingsRegistryProviderServiceConfigurator {

    private volatile String containerName;
    private volatile String cacheName;

    public InfinispanClientMappingsRegistryProviderServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asStringOrNull();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return this;
    }

    @Override
    public ClientMappingsRegistryProvider get() {
        // return new InfinispanClientMappingsRegistryProvider();
        return new InfinispanClientMappingsRegistryProvider(this.containerName, this.cacheName);
    }
}
