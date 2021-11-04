package org.wildfly.clustering.ejb;

/**
 * interface for obtaining ClientMappingsRegistryProvider instances in the legacy case where no distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public interface LegacyClientMappingsRegistryProviderFactory {
    ClientMappingsRegistryProvider createClientMappingsRegistryProvider(String clusterName);
}
