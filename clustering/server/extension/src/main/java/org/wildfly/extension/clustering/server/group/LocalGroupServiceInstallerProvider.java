/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LocalServiceInstallerProvider.class)
public class LocalGroupServiceInstallerProvider extends GroupServiceInstallerProvider implements LocalServiceInstallerProvider {

    public LocalGroupServiceInstallerProvider() {
        super(LocalGroupServiceInstallerFactory.INSTANCE);
    }
}
