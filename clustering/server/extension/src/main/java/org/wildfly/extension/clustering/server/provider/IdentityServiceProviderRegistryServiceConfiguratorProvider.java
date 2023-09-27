/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.extension.clustering.server.IdentityCacheRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentityServiceProviderRegistryServiceConfiguratorProvider extends IdentityCacheRequirementServiceConfiguratorProvider {

    public IdentityServiceProviderRegistryServiceConfiguratorProvider() {
        super(ClusteringCacheRequirement.SERVICE_PROVIDER_REGISTRY, (containerName, cacheName) -> JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "providers", containerName, cacheName));
    }
}
