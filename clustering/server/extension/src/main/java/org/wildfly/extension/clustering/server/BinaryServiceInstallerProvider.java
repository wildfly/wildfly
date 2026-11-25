/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class BinaryServiceInstallerProvider<T> implements Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>> {

    private final BinaryServiceInstallerFactory<T> installerFactory;
    private final Function<BinaryServiceConfiguration, JndiName> jndiNameFactory;

    protected BinaryServiceInstallerProvider(BinaryServiceInstallerFactory<T> installerFactory) {
        this(installerFactory, null);
    }

    protected BinaryServiceInstallerProvider(BinaryServiceInstallerFactory<T> installerFactory, Function<BinaryServiceConfiguration, JndiName> jndiNameFactory) {
        this.installerFactory = installerFactory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        ServiceInstaller installer = this.installerFactory.apply(configuration);
        if (this.jndiNameFactory == null) {
            return List.of(installer);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(configuration).getAbsoluteName());
        ServiceName name = configuration.resolveServiceName(this.installerFactory.getServiceDescriptor());
        return List.of(installer, new BinderServiceInstaller(binding, name));
    }
}
