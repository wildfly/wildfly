/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.util.function.UnaryOperator;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.service.ServiceDependency;
import org.wildfly.service.ServiceInstaller;

/**
 * Transforms a service installer into a singleton service installer.
 * @author Paul Ferraro
 */
public class SingletonDeploymentServiceInstallerTransformer implements UnaryOperator<ServiceInstaller> {

    private final ServiceName name;

    SingletonDeploymentServiceInstallerTransformer(ServiceName name) {
        this.name = name;
    }

    @Override
    public ServiceInstaller apply(ServiceInstaller installer) {
        ServiceDependency<ServiceTargetFactory> factory = ServiceDependency.on(this.name);
        return ServiceInstaller.Builder.of(new ServiceInstaller() {
            @Override
            public ServiceController<?> install(ServiceTarget target) {
                return installer.install(factory.get().createSingletonServiceTarget(target));
            }
        }).requires(factory).build();
    }
}
