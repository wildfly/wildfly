/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processor;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.as.weld.spi.BootstrapDependencyInstaller;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.function.Consumer;

/**
 * @author Martin Kouba
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionsBootstrapDependencyInstaller implements BootstrapDependencyInstaller {
    private static final String CAPABILITY_NAME = "org.wildfly.transactions.global-default-local-provider";

    @Override
    public ServiceName install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, boolean jtsEnabled) {
        final CapabilityServiceSupport capabilities = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (capabilities.hasCapability(CAPABILITY_NAME)) {
            final ServiceName weldTransactionServiceName = deploymentUnit.getServiceName()
                    .append(WeldTransactionServices.SERVICE_NAME);
            final ServiceBuilder<?> sb = serviceTarget.addService(weldTransactionServiceName);
            final Consumer<WeldTransactionServices> weldTransactionServicesConsumer = sb.provides(weldTransactionServiceName);
            // Ensure the local transaction provider is started before we start
            sb.requires(ServiceNames.capabilityServiceName(deploymentUnit, CAPABILITY_NAME));
            sb.setInstance(new WeldTransactionServices(jtsEnabled, weldTransactionServicesConsumer));
            sb.install();

            return weldTransactionServiceName;
        }
        return null;
    }

}
