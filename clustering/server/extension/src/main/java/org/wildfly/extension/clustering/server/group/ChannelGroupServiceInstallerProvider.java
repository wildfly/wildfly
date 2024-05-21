/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ChannelServiceInstallerProvider.class)
public class ChannelGroupServiceInstallerProvider extends GroupServiceInstallerProvider implements ChannelServiceInstallerProvider {

    public ChannelGroupServiceInstallerProvider() {
        super(GroupServiceInstallerFactory.INSTANCE);
    }
}
