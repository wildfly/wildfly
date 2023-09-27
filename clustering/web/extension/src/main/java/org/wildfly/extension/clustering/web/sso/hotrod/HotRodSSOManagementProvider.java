/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.hotrod;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOManagementProvider implements DistributableSSOManagementProvider {

    private final HotRodSSOManagementConfiguration configuration;

    public HotRodSSOManagementProvider(HotRodSSOManagementConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(String name) {
        return new HotRodSSOManagerFactoryServiceConfigurator<>(this.configuration, name);
    }
}
