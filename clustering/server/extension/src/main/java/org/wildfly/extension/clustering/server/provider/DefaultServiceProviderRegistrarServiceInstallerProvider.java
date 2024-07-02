/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;
import org.wildfly.extension.clustering.server.DefaultBinaryServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DefaultCacheServiceInstallerProvider.class)
public class DefaultServiceProviderRegistrarServiceInstallerProvider<T> extends DefaultBinaryServiceInstallerProvider<ServiceProviderRegistrar<T, GroupMember>> implements DefaultCacheServiceInstallerProvider {

    public DefaultServiceProviderRegistrarServiceInstallerProvider() {
        super(new CacheServiceProviderRegistrarServiceInstallerFactory<T>().getServiceDescriptor(), CacheJndiNameFactory.SERVICE_PROVIDER_REGISTRY);
    }
}
