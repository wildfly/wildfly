/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
