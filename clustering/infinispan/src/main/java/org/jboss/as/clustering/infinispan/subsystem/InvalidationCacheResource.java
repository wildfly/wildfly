package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/invalidation-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InvalidationCacheResource extends ClusteredCacheResource {

    public static final PathElement INVALIDATION_CACHE_PATH = PathElement.pathElement(ModelKeys.INVALIDATION_CACHE);
    public static final InvalidationCacheResource INSTANCE = new InvalidationCacheResource();

    // attributes

    public InvalidationCacheResource() {
        super(INVALIDATION_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.INVALIDATION_CACHE),
                InvalidationCacheAdd.INSTANCE,
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
