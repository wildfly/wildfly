package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/local-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LocalCacheResource extends CacheResource {

    public static final PathElement LOCAL_CACHE_PATH = PathElement.pathElement(ModelKeys.LOCAL_CACHE);

    // attributes

    public LocalCacheResource(boolean runtimeRegistration) {
        super(LOCAL_CACHE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.LOCAL_CACHE),
                LocalCacheAdd.INSTANCE,
                CacheRemove.INSTANCE, runtimeRegistration);
    }


}
