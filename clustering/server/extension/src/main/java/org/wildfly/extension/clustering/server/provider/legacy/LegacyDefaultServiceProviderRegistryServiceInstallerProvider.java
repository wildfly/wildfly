/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.extension.clustering.server.DefaultBinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyCacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(DefaultCacheServiceInstallerProvider.class)
public class LegacyDefaultServiceProviderRegistryServiceInstallerProvider<T> extends DefaultBinaryServiceInstallerProvider<org.wildfly.clustering.provider.ServiceProviderRegistry<T>> implements DefaultCacheServiceInstallerProvider {

    public LegacyDefaultServiceProviderRegistryServiceInstallerProvider() {
        super(new LegacyCacheServiceProviderRegistryServiceInstallerFactory<T>().getServiceDescriptor(), LegacyCacheJndiNameFactory.SERVICE_PROVIDER_REGISTRY);
    }
}
