/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.wildfly.security.http.util.sso.SingleSignOnManager;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Provides an installer of a service providing a single sign-on manager for a security domain.
 * @author Paul Ferraro
 */
public interface SingleSignOnManagerServiceInstallerProvider {
    UnaryServiceDescriptor<SingleSignOnManager> SINGLE_SIGN_ON_MANAGER = UnaryServiceDescriptor.of("org.wildfly.undertow.application-security-domain.sso.manager", SingleSignOnManager.class);

    /**
     * Returns a configurator for a service providing container-specific single sign-on management for a security domain.
     * @param configuration the configuration of the security domain's single sign-on management
     * @return a configurator for a service providing a container-specific single sign-on management
     */
    ResourceServiceInstaller getServiceInstaller(SingleSignOnManagerConfiguration configuration);
}
