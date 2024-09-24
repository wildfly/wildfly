/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.network.SocketBindingManager;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

/**
 * Enumerates common requirements for clustering resources.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link CommonServiceDescriptor}.
 */
@Deprecated(forRemoval = true)
public enum CommonRequirement implements Requirement, ServiceNameFactoryProvider {
    ELYTRON("org.wildfly.security.elytron", Void.class),
    MBEAN_SERVER(MBeanServerResolver.SERVICE_DESCRIPTOR),
    NAMING_STORE(NamingService.CAPABILITY_NAME, NamingStore.class),
    PATH_MANAGER(PathManager.SERVICE_DESCRIPTOR),
    SOCKET_BINDING_MANAGER(SocketBindingManager.SERVICE_DESCRIPTOR),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory = new RequirementServiceNameFactory(this);

    CommonRequirement(NullaryServiceDescriptor<?> descriptor) {
        this(descriptor.getName(), descriptor.getType());
    }

    CommonRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public ServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
