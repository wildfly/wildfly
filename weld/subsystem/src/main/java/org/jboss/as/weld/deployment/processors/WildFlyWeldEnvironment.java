/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.security.spi.SecurityServices;

/**
 * Since not all Weld subsystem submodules must be always present (e.g. when using WildFly Swarm) we cannot use {@link Environments#EE_INJECT}.
 *
 * @author Martin Kouba
 */
class WildFlyWeldEnvironment implements Environment {

    static final WildFlyWeldEnvironment INSTANCE = new WildFlyWeldEnvironment();

    private final Set<Class<? extends Service>> requiredDeploymentServices;

    private final Set<Class<? extends Service>> requiredBeanDeploymentArchiveServices;

    private WildFlyWeldEnvironment() {
        this.requiredDeploymentServices = Collections.singleton(SecurityServices.class);
        Set<Class<? extends Service>> beanDeploymentArchiveServices = new HashSet<>();
        beanDeploymentArchiveServices.add(ResourceLoader.class);
        beanDeploymentArchiveServices.add(ResourceInjectionServices.class);
        this.requiredBeanDeploymentArchiveServices = Collections.unmodifiableSet(beanDeploymentArchiveServices);
    }

    @Override
    public Set<Class<? extends Service>> getRequiredDeploymentServices() {
        return requiredDeploymentServices;
    }

    @Override
    public Set<Class<? extends Service>> getRequiredBeanDeploymentArchiveServices() {
        return requiredBeanDeploymentArchiveServices;
    }

}
