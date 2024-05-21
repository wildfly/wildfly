/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import org.jgroups.Address;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;
import org.wildfly.clustering.server.jgroups.dispatcher.ChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;
import org.wildfly.extension.clustering.server.group.legacy.LegacyChannelGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(ChannelServiceInstallerProvider.class)
public class LegacyChannelCommandDispatcherFactoryServiceInstallerProvider extends LegacyCommandDispatcherFactoryServiceInstallerProvider implements ChannelServiceInstallerProvider {

    public LegacyChannelCommandDispatcherFactoryServiceInstallerProvider() {
        super(new LegacyCommandDispatcherFactoryServiceInstallerFactory<>(ChannelCommandDispatcherFactory.class, LegacyChannelCommandDispatcherFactory::new));
    }

    private static class LegacyChannelCommandDispatcherFactory implements LegacyCommandDispatcherFactory<Address, ChannelGroupMember> {
        private final ChannelCommandDispatcherFactory factory;

        LegacyChannelCommandDispatcherFactory(ChannelCommandDispatcherFactory factory) {
            this.factory = factory;
        }

        @Override
        public ChannelCommandDispatcherFactory unwrap() {
            return this.factory;
        }

        @Override
        public LegacyChannelGroup getGroup() {
            return LegacyChannelGroup.wrap(this.factory.getGroup());
        }
    }
}
