/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class BiUnaryServiceInstallerProvider<T> implements BiFunction<CapabilityServiceSupport, Map.Entry<String, String>, Iterable<ServiceInstaller>> {

    private final UnaryServiceDescriptor<T> descriptor;
    private final BiFunction<CapabilityServiceSupport, Map.Entry<String, String>, ServiceInstaller> installerFactory;
    private final Function<String, JndiName> jndiNameFactory;

    protected BiUnaryServiceInstallerProvider(UnaryServiceDescriptor<T> descriptor, BiFunction<CapabilityServiceSupport, Map.Entry<String, String>, ServiceInstaller> installerFactory, Function<String, JndiName> jndiNameFactory) {
        this.descriptor = descriptor;
        this.installerFactory = installerFactory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceInstaller> apply(CapabilityServiceSupport support, Map.Entry<String, String> entry) {
        ServiceInstaller installer = this.installerFactory.apply(support, entry);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(entry.getKey()).getAbsoluteName());
        ServiceName name = support.getCapabilityServiceName(this.descriptor, entry.getKey());
        return List.of(installer, new BinderServiceInstaller(binding, name));
    }
}
