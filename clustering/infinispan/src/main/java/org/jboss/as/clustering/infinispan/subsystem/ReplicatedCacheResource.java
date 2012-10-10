package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.ResolvePathHandler;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/replicated-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ReplicatedCacheResource extends SharedCacheResource {

    public static final PathElement REPLICATED_CACHE_PATH = PathElement.pathElement(ModelKeys.REPLICATED_CACHE);

    // attributes

    public ReplicatedCacheResource(final ResolvePathHandler resolvePathHandler) {
        super(REPLICATED_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.REPLICATED_CACHE),
                ReplicatedCacheAdd.INSTANCE,
                CacheRemove.INSTANCE, resolvePathHandler);
    }
}
