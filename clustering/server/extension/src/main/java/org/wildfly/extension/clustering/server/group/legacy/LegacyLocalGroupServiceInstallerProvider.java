/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LocalServiceInstallerProvider.class)
public class LegacyLocalGroupServiceInstallerProvider extends LegacyGroupServiceInstallerProvider implements LocalServiceInstallerProvider {

    public LegacyLocalGroupServiceInstallerProvider() {
        super(new LegacyGroupServiceInstallerFactory<>(LocalGroup.class, LegacyLocalGroup::wrap));
    }
}
