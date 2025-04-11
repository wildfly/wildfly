/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.util.function.Function;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyGroupServiceInstallerFactory<M extends GroupMember, G extends Group<M>> implements Function<String, ServiceInstaller> {

    private final Class<G> groupType;
    private final Function<G, org.wildfly.clustering.group.Group> wrapper;

    LegacyGroupServiceInstallerFactory(Class<G> groupType, Function<G, org.wildfly.clustering.group.Group> wrapper) {
        this.groupType = groupType;
        this.wrapper = wrapper;
    }

    @Override
    public ServiceInstaller apply(String name) {
        ServiceDependency<G> group = ServiceDependency.on(ClusteringServiceDescriptor.GROUP, name).map(this.groupType::cast);
        return ServiceInstaller.builder(this.wrapper, group)
                .provides(ServiceNameFactory.resolveServiceName(LegacyClusteringServiceDescriptor.GROUP, name))
                .requires(group)
                .build();
    }
}
