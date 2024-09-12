/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.user;

import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public interface DistributableUserManagementProvider {
    NullaryServiceDescriptor<DistributableUserManagementProvider> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.web.default-single-sign-on-management-provider", DistributableUserManagementProvider.class);
    UnaryServiceDescriptor<DistributableUserManagementProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.single-sign-on-management-provider", DEFAULT_SERVICE_DESCRIPTOR);

    Iterable<ServiceInstaller> getServiceInstallers(String name);
}
