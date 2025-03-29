/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class DefaultBinaryServiceInstallerProvider<T> implements Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>> {

    private final BinaryServiceDescriptor<T> descriptor;
    private final Function<BinaryServiceConfiguration, JndiName> jndiNameFactory;

    protected DefaultBinaryServiceInstallerProvider(BinaryServiceDescriptor<T> descriptor, Function<BinaryServiceConfiguration, JndiName> jndiNameFactory) {
        this.descriptor = descriptor;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        ServiceName name = configuration.withChildName(null).resolveServiceName(this.descriptor);
        List<ServiceInstaller> installers = new ArrayList<>(2);
        installers.add(ServiceInstaller.builder(configuration.getServiceDependency(this.descriptor)).provides(name).build());
        if (!configuration.getChildName().equals(ModelDescriptionConstants.DEFAULT)) {
            ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(configuration.withChildName(ModelDescriptionConstants.DEFAULT)).getAbsoluteName());
            installers.add(new BinderServiceInstaller(binding, name));
        }
        return installers;
    }
}
