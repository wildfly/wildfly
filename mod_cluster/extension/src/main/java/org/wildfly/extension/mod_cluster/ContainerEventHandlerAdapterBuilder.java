package org.wildfly.extension.mod_cluster;

import java.time.Duration;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

public interface ContainerEventHandlerAdapterBuilder {
    ServiceBuilder<?> build(ServiceTarget target, CapabilityServiceSupport serviceSupport, String connector, Duration statusInterval);
}
