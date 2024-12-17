/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Provides timer management service installation mechanics for a component.
 * @author Paul Ferraro
 */
public interface TimerManagementProvider {
    UnaryServiceDescriptor<TimerManagementProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.ejb.timer-management-provider", TimerManagementProvider.class);

    <I> Iterable<ServiceInstaller> getTimerManagerFactoryServiceInstallers(ServiceName name, TimerManagerFactoryConfiguration<I> config);
}
