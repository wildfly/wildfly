/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ChannelServiceInstallerProvider.class)
public class ChannelCommandDispatcherServiceInstallerProvider extends CommandDispatcherFactoryServiceInstallerProvider implements ChannelServiceInstallerProvider {

    public ChannelCommandDispatcherServiceInstallerProvider() {
        super(ChannelCommandDispatcherFactoryServiceInstallerFactory.INSTANCE);
    }
}
