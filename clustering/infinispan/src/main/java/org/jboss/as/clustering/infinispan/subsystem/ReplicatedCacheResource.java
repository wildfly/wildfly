package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/replicated-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ReplicatedCacheResource extends SharedCacheResource {

    public static final PathElement REPLICATED_CACHE_PATH = PathElement.pathElement(ModelKeys.REPLICATED_CACHE);

    // attributes

    public ReplicatedCacheResource() {
        super(REPLICATED_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.REPLICATED_CACHE),
                ReplicatedCacheAdd.INSTANCE,
                CacheRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
