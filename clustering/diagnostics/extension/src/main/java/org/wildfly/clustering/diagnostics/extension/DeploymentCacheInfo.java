package org.wildfly.clustering.diagnostics.extension;

/**
 * Describes an Infinispan cache.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class DeploymentCacheInfo {

    private final String container;
    private final String cache;

    public DeploymentCacheInfo(String container, String cache) {
        this.container = container;
        this.cache = cache;
    }

    public String getContainer() {
        return container;
    }

    public String getCache() {
        return cache;
    }

    @Override
    public String toString() {
        return "DeploymentCacheInfo: " +
                "container = " + container +
                ", " +
                "cache = " + cache ;
    }
}
