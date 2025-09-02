/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.user;

import java.net.URI;
import java.util.Map;

import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public interface DistributableUserManagementProvider {
    NullaryServiceDescriptor<DistributableUserManagementProvider> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.web.default-single-sign-on-management-provider", DistributableUserManagementProvider.class);
    UnaryServiceDescriptor<DistributableUserManagementProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.single-sign-on-management-provider", DEFAULT_SERVICE_DESCRIPTOR);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>> USER_MANAGER_FACTORY = UnaryServiceDescriptor.of("user-manager-factory", (Class<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>>) (Class<?>) UserManagerFactory.class);

    /**
     * Returns a number of installers of services, one of which providing a user manager factory, for a given application security domain.
     * @param name an application security domain name
     * @return a number of service installers
     */
    Iterable<ServiceInstaller> getServiceInstallers(String name);
}
