package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.ResolvePathHandler;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/invalidation-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InvalidationCacheResource extends ClusteredCacheResource {

    public static final PathElement INVALIDATION_CACHE_PATH = PathElement.pathElement(ModelKeys.INVALIDATION_CACHE);

    // attributes

    public InvalidationCacheResource(final ResolvePathHandler resolvePathHandler) {
        super(INVALIDATION_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.INVALIDATION_CACHE),
                InvalidationCacheAdd.INSTANCE,
                CacheRemove.INSTANCE, resolvePathHandler);
    }
}
