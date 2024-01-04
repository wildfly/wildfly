/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.spi.BootstrapDependencyInstaller;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.security.spi.SecurityServices;

import java.util.function.Consumer;

/**
 * @author Martin Kouba
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class SecurityBootstrapDependencyInstaller implements BootstrapDependencyInstaller {

    @Override
    public ServiceName install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, boolean jtsEnabled) {
        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldSecurityServices.SERVICE_NAME);
        final CapabilityServiceSupport capabilities = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        final ServiceBuilder<?> sb = serviceTarget.addService(serviceName);
        final Consumer<SecurityServices> securityServicesConsumer = sb.provides(serviceName);

        sb.setInstance(new WeldSecurityServices(securityServicesConsumer));
        sb.install();
        return serviceName;
    }

}
