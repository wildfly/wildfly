/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;

/**
 * A configurator for a service dependency.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @param <T> the type of the service that is being configured
 */
public interface DependencyConfigurator<T extends Service> {

    /**
     * Configure the dependency on the service builder.
     *
     * @param serviceBuilder the service builder
     * @param service
     */
    void configureDependency(ServiceBuilder<?> serviceBuilder, final T service) throws DeploymentUnitProcessingException;
}
