/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.DeploymentConfiguration;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Provides the {@link ServiceName} for a service providing a bean group manager.
 * @author Paul Ferraro
 */
public class BeanGroupManagerServiceNameProvider extends SimpleServiceNameProvider {

    public BeanGroupManagerServiceNameProvider(DeploymentConfiguration config) {
        super(ServiceName.JBOSS.append("clustering", "ejb", "manager", config.getDeploymentName()));
    }
}
