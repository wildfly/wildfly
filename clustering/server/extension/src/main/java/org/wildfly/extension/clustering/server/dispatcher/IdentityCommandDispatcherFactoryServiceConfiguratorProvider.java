/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.extension.clustering.server.GroupJndiNameFactory;
import org.wildfly.extension.clustering.server.IdentityGroupRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityGroupServiceConfiguratorProvider.class)
public class IdentityCommandDispatcherFactoryServiceConfiguratorProvider extends IdentityGroupRequirementServiceConfiguratorProvider {

    public IdentityCommandDispatcherFactoryServiceConfiguratorProvider() {
        super(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY, GroupJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
