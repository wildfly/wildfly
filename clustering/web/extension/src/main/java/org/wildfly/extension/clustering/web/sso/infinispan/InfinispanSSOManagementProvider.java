/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.infinispan;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;

/**
 * An Infinispan cache-based {@link DistributableSSOManagementProvider}.
 * @author Paul Ferraro
 */
public class InfinispanSSOManagementProvider implements DistributableSSOManagementProvider {

    private final InfinispanSSOManagementConfiguration configuration;

    public InfinispanSSOManagementProvider(InfinispanSSOManagementConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(String name) {
        return new InfinispanSSOManagerFactoryServiceConfigurator<>(this.configuration, name);
    }
}
