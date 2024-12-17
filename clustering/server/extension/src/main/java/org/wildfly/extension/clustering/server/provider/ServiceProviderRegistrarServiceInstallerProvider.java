/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.extension.clustering.server.BinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrarServiceInstallerProvider<T> extends BinaryServiceInstallerProvider<ServiceProviderRegistrar<T, GroupMember>> {

    public ServiceProviderRegistrarServiceInstallerProvider(BinaryServiceInstallerFactory<ServiceProviderRegistrar<T, GroupMember>> installerFactory) {
        super(installerFactory, CacheJndiNameFactory.SERVICE_PROVIDER_REGISTRY);
    }
}
