/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.DefaultUnaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.ChannelJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DefaultChannelServiceInstallerProvider.class)
public class DefaultGroupServiceInstallerProvider extends DefaultUnaryServiceInstallerProvider<Group<GroupMember>> implements DefaultChannelServiceInstallerProvider {

    public DefaultGroupServiceInstallerProvider() {
        super(ClusteringServiceDescriptor.GROUP, ChannelJndiNameFactory.GROUP);
    }
}
