/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Configures the dependencies of a {@link org.jboss.msc.Service}.
 * @author Paul Ferraro
 */
public interface ServiceConfigurator extends ServiceNameProvider {

    /**
     * Adds and configures a {@link org.jboss.msc.Service}.
     * @param target a service target
     * @return the builder of the service.
     */
    ServiceBuilder<?> build(ServiceTarget target);
}
