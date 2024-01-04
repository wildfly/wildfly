/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.extension.clustering.server.IdentityCacheRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentityCacheGroupServiceConfiguratorProvider extends IdentityCacheRequirementServiceConfiguratorProvider {

    public IdentityCacheGroupServiceConfiguratorProvider() {
        super(ClusteringCacheRequirement.GROUP);
    }
}
