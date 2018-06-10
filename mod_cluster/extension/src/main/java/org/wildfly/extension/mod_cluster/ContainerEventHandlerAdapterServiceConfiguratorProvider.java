package org.wildfly.extension.mod_cluster;

import java.time.Duration;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * Creates builder of a service that triggers container events for use by {@link org.jboss.modcluster.container.ContainerEventHandler}.
 * @author Paul Ferraro
 */
public interface ContainerEventHandlerAdapterServiceConfiguratorProvider {
    CapabilityServiceConfigurator getServiceConfigurator(String connector, Duration statusInterval);
}
