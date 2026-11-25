/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.extension.clustering.server.BinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyCacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyServiceProviderRegistryServiceInstallerProvider<T> extends BinaryServiceInstallerProvider<org.wildfly.clustering.provider.ServiceProviderRegistry<T>> {

    public LegacyServiceProviderRegistryServiceInstallerProvider(BinaryServiceInstallerFactory<org.wildfly.clustering.provider.ServiceProviderRegistry<T>> installerFactory) {
        super(installerFactory, LegacyCacheJndiNameFactory.SERVICE_PROVIDER_REGISTRY);
    }
}
