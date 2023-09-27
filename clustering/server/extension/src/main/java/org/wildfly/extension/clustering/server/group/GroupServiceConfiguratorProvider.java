/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.GroupCapabilityServiceConfiguratorFactory;
import org.wildfly.extension.clustering.server.GroupJndiNameFactory;
import org.wildfly.extension.clustering.server.GroupRequirementServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a {@link Group} service created from a specified factory.
 * @author Paul Ferraro
 */
public class GroupServiceConfiguratorProvider extends GroupRequirementServiceConfiguratorProvider<Group> {

    public GroupServiceConfiguratorProvider(GroupCapabilityServiceConfiguratorFactory<Group> factory) {
        super(ClusteringRequirement.GROUP, factory, GroupJndiNameFactory.GROUP);
    }
}
