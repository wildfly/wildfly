/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.util.function.Function;

import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.LegacyChannelJndiNameFactory;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyGroupServiceInstallerProvider extends UnaryServiceInstallerProvider<org.wildfly.clustering.group.Group> {

    LegacyGroupServiceInstallerProvider(Function<String, ServiceInstaller> installerFactory) {
        super(LegacyClusteringServiceDescriptor.GROUP, installerFactory, LegacyChannelJndiNameFactory.GROUP);
    }
}
