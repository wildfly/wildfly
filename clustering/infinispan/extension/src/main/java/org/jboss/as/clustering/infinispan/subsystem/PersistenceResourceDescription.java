/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface PersistenceResourceDescription extends CacheComponentResourceDescription<PersistenceConfiguration, PersistenceConfigurationBuilder> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("store", value);
    }

    @Override
    default PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    BinaryServiceDescriptor<PersistenceConfiguration> SERVICE_DESCRIPTOR = CacheComponentResourceDescription.createServiceDescriptor(pathElement(PathElement.WILDCARD_VALUE), PersistenceConfiguration.class);
    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    @Override
    default BinaryServiceDescriptor<PersistenceConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    default RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }
}
