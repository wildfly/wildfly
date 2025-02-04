/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.remote;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * interface defining ModuleAvailabilityRegistrarProvider instances, used to install configured module availability registrar services.
 *
 * @author Richard Achmatowicz
 */
public interface ModuleAvailabilityRegistrarProvider {
    NullaryServiceDescriptor<ModuleAvailabilityRegistrarProvider> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.ejb.module-availability-registrar-provider", ModuleAvailabilityRegistrarProvider.class);

    Iterable<ServiceInstaller> getServiceInstallers(CapabilityServiceSupport support);
}
