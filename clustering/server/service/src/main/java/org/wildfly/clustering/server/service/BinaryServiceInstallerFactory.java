/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.function.BiFunction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Creates a service installer for a given service configuration.
 * @author Paul Ferraro
 */
public interface BinaryServiceInstallerFactory<T> extends BiFunction<CapabilityServiceSupport, BinaryServiceConfiguration, ServiceInstaller> {

    /**
     * Returns the descriptor of the service created by this factory.
     * @return a service descriptor
     */
    BinaryServiceDescriptor<T> getServiceDescriptor();
}
