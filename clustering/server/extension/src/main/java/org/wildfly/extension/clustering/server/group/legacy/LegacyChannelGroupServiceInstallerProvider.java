/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.jgroups.ChannelGroup;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(ChannelServiceInstallerProvider.class)
public class LegacyChannelGroupServiceInstallerProvider extends LegacyGroupServiceInstallerProvider implements ChannelServiceInstallerProvider {

    public LegacyChannelGroupServiceInstallerProvider() {
        super(new LegacyGroupServiceInstallerFactory<>(ChannelGroup.class, LegacyChannelGroup::wrap));
    }
}
