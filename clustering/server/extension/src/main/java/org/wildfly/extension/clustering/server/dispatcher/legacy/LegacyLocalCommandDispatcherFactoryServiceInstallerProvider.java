/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.local.dispatcher.LocalCommandDispatcherFactory;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;
import org.wildfly.extension.clustering.server.group.legacy.LegacyLocalGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LocalServiceInstallerProvider.class)
public class LegacyLocalCommandDispatcherFactoryServiceInstallerProvider extends LegacyCommandDispatcherFactoryServiceInstallerProvider implements LocalServiceInstallerProvider {

    public LegacyLocalCommandDispatcherFactoryServiceInstallerProvider() {
        super(new LegacyCommandDispatcherFactoryServiceInstallerFactory<>(LocalCommandDispatcherFactory.class, LegacyLocalCommandDispatcherFactory::new));
    }

    private static class LegacyLocalCommandDispatcherFactory implements LegacyCommandDispatcherFactory<String, LocalGroupMember> {
        private final LocalCommandDispatcherFactory factory;

        LegacyLocalCommandDispatcherFactory(LocalCommandDispatcherFactory factory) {
            this.factory = factory;
        }

        @Override
        public LocalCommandDispatcherFactory unwrap() {
            return this.factory;
        }

        @Override
        public LegacyLocalGroup getGroup() {
            return LegacyLocalGroup.wrap(this.factory.getGroup());
        }
    }
}
