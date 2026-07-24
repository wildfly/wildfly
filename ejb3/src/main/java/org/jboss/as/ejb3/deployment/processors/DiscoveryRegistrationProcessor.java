/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.local.LocalEJBDiscoveryProviderService;
import org.jboss.as.ejb3.profile.RemotingProfileService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.protocol.remote.RemoteEJBDiscoveryConfigurator;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.Functions;
import org.wildfly.discovery.Discovery;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.httpclient.ejb.HttpDiscoveryConfigurator;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Processor responsible for ensuring that the discovery service for each deployment unit exists.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rachmato@ibm.com">Richard Achmatowicz</a>
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
        final Supplier<RemotingProfileService> remotingProfileServiceSupplier = profileServiceName != null ? builder.requires(profileServiceName) : Functions.constantSupplier(null);
        // only add local discovery service dependency if the context is configured to use the local EJB receiver & we are not app client
        final EJBClientDescriptorMetaData ejbClientDescriptorMetaData = deploymentUnit.getAttachment(Attachments.EJB_CLIENT_METADATA);
        final boolean useLocalReceiver = ejbClientDescriptorMetaData == null || ejbClientDescriptorMetaData.isLocalReceiverExcluded() != Boolean.TRUE;
        // add in local discovery
        final Supplier<DiscoveryProvider> localDiscoveryProviderSupplier = (useLocalReceiver && !appClient) ? builder.requires(LocalEJBDiscoveryProviderService.SERVICE_NAME) : Functions.constantSupplier(null);
        final DiscoveryService discoveryService = new DiscoveryService(discoveryConsumer, remotingProfileServiceSupplier, localDiscoveryProviderSupplier);
        // configure a discovery provider based on RemotingEJBDiscoveryProvider
        new RemoteEJBDiscoveryConfigurator().configure(discoveryService.getDiscoveryProviderConsumer(), registryProvider -> {});
        // configure a discovery provider based on HttpEJBDiscoveryProvider
        new HttpDiscoveryConfigurator().configure(discoveryService.getDiscoveryProviderConsumer(), registryProvider -> {});
        builder.setInstance(discoveryService);
        builder.install();
    }
}
