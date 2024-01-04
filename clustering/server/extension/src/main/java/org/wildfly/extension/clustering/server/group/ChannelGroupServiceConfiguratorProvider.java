/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.service.DistributedGroupServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a channel-based {@link Group} service.
 * @author Paul Ferraro
 */
@MetaInfServices(DistributedGroupServiceConfiguratorProvider.class)
public class ChannelGroupServiceConfiguratorProvider extends GroupServiceConfiguratorProvider implements DistributedGroupServiceConfiguratorProvider {

    public ChannelGroupServiceConfiguratorProvider() {
        super((registry, group) -> new ChannelGroupServiceConfigurator(registry, group));
    }
}
