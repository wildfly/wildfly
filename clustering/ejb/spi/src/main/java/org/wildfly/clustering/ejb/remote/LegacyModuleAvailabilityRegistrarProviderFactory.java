/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.remote;

/**
 * interface for obtaining ModuleAvailabilityRegistrarProvider instances in the legacy case where no distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@Deprecated
public interface LegacyModuleAvailabilityRegistrarProviderFactory {
    ModuleAvailabilityRegistrarProvider createModuleAvailabilityRegistrarProvider(String clusterName);
}
