/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.sso.elytron;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementProvider;

/**
 * Singleton reference to a non-distributable {@link SecurityDomainSingleSignOnManagementProvider}.
 * @author Paul Ferraro
 */
public enum NonDistributableSingleSignOnManagementProvider implements SecurityDomainSingleSignOnManagementProvider {
    INSTANCE;

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration) {
        return new DefaultSingleSignOnManagerServiceConfigurator(name, configuration);
    }
}
