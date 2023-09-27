/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.sso;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public interface DistributableSSOManagementProvider {
    CapabilityServiceConfigurator getServiceConfigurator(String name);
}
