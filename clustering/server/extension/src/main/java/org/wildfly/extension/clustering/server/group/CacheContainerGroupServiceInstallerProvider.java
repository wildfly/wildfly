/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(CacheContainerServiceInstallerProvider.class)
public class CacheContainerGroupServiceInstallerProvider implements CacheContainerServiceInstallerProvider {

    private final UnaryServiceInstallerProvider<Group<GroupMember>> provider = new GroupServiceInstallerProvider(GroupServiceInstallerFactory.INSTANCE);

    @Override
    public Iterable<ServiceInstaller> apply(String name, String context) {
        return this.provider.apply(name);
    }
}
