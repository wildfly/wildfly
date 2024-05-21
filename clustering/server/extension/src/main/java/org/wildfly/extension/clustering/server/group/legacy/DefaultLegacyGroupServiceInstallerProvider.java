/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.DefaultUnaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyChannelJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(DefaultChannelServiceInstallerProvider.class)
public class DefaultLegacyGroupServiceInstallerProvider extends DefaultUnaryServiceInstallerProvider<org.wildfly.clustering.group.Group> implements DefaultChannelServiceInstallerProvider {

    public DefaultLegacyGroupServiceInstallerProvider() {
        super(LegacyClusteringServiceDescriptor.GROUP, LegacyChannelJndiNameFactory.GROUP);
    }
}
