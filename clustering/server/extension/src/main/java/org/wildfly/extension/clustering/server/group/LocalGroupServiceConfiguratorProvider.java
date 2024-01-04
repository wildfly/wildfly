/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.group.Group;

/**
 * Provides the requisite builders for a non-clustered {@link Group} service.
 * @author Paul Ferraro
 */
@MetaInfServices(org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider.class)
public class LocalGroupServiceConfiguratorProvider extends GroupServiceConfiguratorProvider implements org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider {

    public LocalGroupServiceConfiguratorProvider() {
        super((name, group) -> new LocalGroupServiceConfigurator(name));
    }
}
