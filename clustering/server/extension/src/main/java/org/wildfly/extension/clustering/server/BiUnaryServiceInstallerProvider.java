/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class BiUnaryServiceInstallerProvider<T> implements BiFunction<String, String, Iterable<ServiceInstaller>> {

    private final UnaryServiceDescriptor<T> descriptor;
    private final BiFunction<String, String, ServiceInstaller> installerFactory;
    private final Function<String, JndiName> jndiNameFactory;

    protected BiUnaryServiceInstallerProvider(UnaryServiceDescriptor<T> descriptor, BiFunction<String, String, ServiceInstaller> installerFactory, Function<String, JndiName> jndiNameFactory) {
        this.descriptor = descriptor;
        this.installerFactory = installerFactory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceInstaller> apply(String name, String context) {
        ServiceInstaller installer = this.installerFactory.apply(name, context);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(name).getAbsoluteName());
        return List.of(installer, new BinderServiceInstaller(binding, ServiceNameFactory.resolveServiceName(this.descriptor, name)));
    }
}
