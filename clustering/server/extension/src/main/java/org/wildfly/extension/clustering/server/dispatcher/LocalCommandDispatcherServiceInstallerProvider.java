/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LocalServiceInstallerProvider.class)
public class LocalCommandDispatcherServiceInstallerProvider extends CommandDispatcherFactoryServiceInstallerProvider implements LocalServiceInstallerProvider {

    public LocalCommandDispatcherServiceInstallerProvider() {
        super(LocalCommandDispatcherFactoryServiceInstallerFactory.INSTANCE);
    }
}
