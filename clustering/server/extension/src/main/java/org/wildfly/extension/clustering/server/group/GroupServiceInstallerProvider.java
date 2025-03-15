/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import java.util.function.Function;

import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.ChannelJndiNameFactory;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class GroupServiceInstallerProvider extends UnaryServiceInstallerProvider<Group<GroupMember>> {

    public GroupServiceInstallerProvider(Function<String, ServiceInstaller> installerFactory) {
        super(ClusteringServiceDescriptor.GROUP, installerFactory, ChannelJndiNameFactory.GROUP);
    }
}
