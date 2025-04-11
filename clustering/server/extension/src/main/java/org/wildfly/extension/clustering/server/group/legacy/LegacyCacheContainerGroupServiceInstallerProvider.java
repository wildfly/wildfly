/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(CacheContainerServiceInstallerProvider.class)
public class LegacyCacheContainerGroupServiceInstallerProvider implements CacheContainerServiceInstallerProvider {

    private final UnaryServiceInstallerProvider<org.wildfly.clustering.group.Group> provider = new LegacyGroupServiceInstallerProvider(new LegacyGroupServiceInstallerFactory<>(CacheContainerGroup.class, LegacyCacheContainerGroup::wrap));

    @Override
    public Iterable<ServiceInstaller> apply(String name, String context) {
        return this.provider.apply(name);
    }
}
