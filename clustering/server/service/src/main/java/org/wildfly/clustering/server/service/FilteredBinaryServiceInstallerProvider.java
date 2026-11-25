/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * SPI for providing select services on behalf of a cache.
 * @author Paul Ferraro
 */
public class FilteredBinaryServiceInstallerProvider implements Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>> {

    private final Set<? extends BinaryServiceDescriptor<?>> descriptors;

    public FilteredBinaryServiceInstallerProvider(Set<? extends BinaryServiceDescriptor<?>> descriptors) {
        this.descriptors = descriptors;
    }

    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        @SuppressWarnings("unchecked")
        Class<BinaryServiceInstallerFactory<Object>> factoryClass = (Class<BinaryServiceInstallerFactory<Object>>) (Class<?>) BinaryServiceInstallerFactory.class;
        return ServiceLoader.load(factoryClass, factoryClass.getClassLoader()).stream().map(ServiceLoader.Provider::get).filter(factory -> this.descriptors.contains(factory.getServiceDescriptor())).map(factory -> factory.apply(configuration)).collect(Collectors.toList());
    }
}
