/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.bootstrap.api.Service;

/**
 * Allows to install a Weld {@link Service} as a MSC {@link org.jboss.msc.service.Service}.
 * <p>
 * The service installed is later added as a dependency of the Weld bootstrap service.
 *
 * @author Martin Kouba
 */
public interface BootstrapDependencyInstaller {

    /**
     *
     * @param serviceTarget
     * @param deploymentUnit
     * @param jtsEnabled
     * @return the service name
     */
    ServiceName install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, boolean jtsEnabled);

}
