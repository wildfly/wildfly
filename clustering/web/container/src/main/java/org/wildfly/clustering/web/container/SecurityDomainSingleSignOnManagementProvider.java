/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;

/**
 * Container-specific single sign-on management provider for a security domain.
 * @author Paul Ferraro
 */
public interface SecurityDomainSingleSignOnManagementProvider {

    /**
     * Returns a configurator for a service providing container-specific single sign-on management for a security domain.
     * @param name the service name of the single sign-on management
     * @param configuration the configuration of the security domain's single sign-on management
     * @return a configurator for a service providing a container-specific single sign-on management
     */
    CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration);
}
