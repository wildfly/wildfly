/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.RemotingProfileService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.protocol.remote.RemoteEJBDiscoveryConfigurator;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.discovery.Discovery;
import org.wildfly.httpclient.ejb.HttpDiscoveryConfigurator;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Processor responsible for ensuring that the discovery service for each deployment unit exists.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DiscoveryRegistrationProcessor implements DeploymentUnitProcessor {
    private final boolean appClient;

    public DiscoveryRegistrationProcessor(final boolean appClient) {
        this.appClient = appClient;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // we only process top level deployment units
        if (deploymentUnit.getParent() != null) {
            return;
        }

        final ServiceName profileServiceName = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_REMOTING_PROFILE_SERVICE_NAME);
        final ServiceName discoveryServiceName = DiscoveryService.BASE_NAME.append(deploymentUnit.getName());
        final ServiceBuilder<?> builder = phaseContext.getServiceTarget().addService(discoveryServiceName);
        final Consumer<Discovery> discoveryConsumer = builder.provides(discoveryServiceName);
        final Supplier<RemotingProfileService> remotingProfileServiceSupplier = profileServiceName != null ? builder.requires(profileServiceName) : null;
        // only add association service dependency if the context is configured to use the local EJB receiver & we are not app client
        final EJBClientDescriptorMetaData ejbClientDescriptorMetaData = deploymentUnit.getAttachment(Attachments.EJB_CLIENT_METADATA);
        final boolean useLocalReceiver = ejbClientDescriptorMetaData == null || ejbClientDescriptorMetaData.isLocalReceiverExcluded() != Boolean.TRUE;
        final Supplier<AssociationService> associationServiceSupplier = (useLocalReceiver && !appClient) ? builder.requires(AssociationService.SERVICE_NAME) : null;
        final DiscoveryService discoveryService = new DiscoveryService(discoveryConsumer, remotingProfileServiceSupplier, associationServiceSupplier);
        new RemoteEJBDiscoveryConfigurator().configure(discoveryService.getDiscoveryProviderConsumer(), registryProvider -> {});
        new HttpDiscoveryConfigurator().configure(discoveryService.getDiscoveryProviderConsumer(), registryProvider -> {});
        builder.setInstance(discoveryService);
        builder.install();
    }
}
