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

package org.wildfly.extension.undertow.deployment;

import io.undertow.server.session.InMemorySessionManager;
import io.undertow.servlet.api.SessionManagerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ServiceController.Mode;
import org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilderProvider;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.session.SimpleDistributableSessionManagerConfiguration;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecBuilder;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor {
    private final String defaultServerName;

    public SharedSessionManagerDeploymentProcessor(String defaultServerName) {
        this.defaultServerName = defaultServerName;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(UndertowAttachments.SHARED_SESSION_MANAGER_CONFIG);
        if (sharedConfig == null) return;

        String deploymentName = (deploymentUnit.getParent() == null) ? deploymentUnit.getName() : String.join(".", deploymentUnit.getParent().getName(), deploymentUnit.getName());
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        String serverName = Optional.ofNullable(warMetaData).map(metaData -> metaData.getMergedJBossWebMetaData().getServerInstanceName())
                .orElse(Optional.ofNullable(DefaultDeploymentMappingProvider.instance().getMapping(deploymentName)).map(Map.Entry::getKey).orElse(this.defaultServerName));

        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ServiceTarget target = phaseContext.getServiceTarget();
        ServiceName deploymentServiceName = deploymentUnit.getServiceName();

        ServiceName managerServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        CapabilityServiceBuilder<SessionManagerFactory> factoryBuilder = DistributableSessionManagerFactoryBuilderProvider.INSTANCE
                .map(provider -> provider.getBuilder(managerServiceName, new SimpleDistributableSessionManagerConfiguration(sharedConfig, serverName, deploymentUnit.getName(), module)))
                .orElseGet(() -> {
                    InMemorySessionManager manager = new InMemorySessionManager(deploymentUnit.getName(), sharedConfig.getMaxActiveSessions());
                    if (sharedConfig.getSessionConfig() != null) {
                        if (sharedConfig.getSessionConfig().getSessionTimeoutSet()) {
                            manager.setDefaultSessionTimeout(sharedConfig.getSessionConfig().getSessionTimeout());
                        }
                    }
                    return new SimpleCapabilityServiceBuilder<>(managerServiceName, new ImmediateSessionManagerFactory(manager));
                });

        ServiceName codecServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME);
        CapabilityServiceBuilder<SessionIdentifierCodec> codecBuilder = DistributableSessionIdentifierCodecBuilderProvider.INSTANCE
                .map(provider -> provider.getDeploymentBuilder(codecServiceName, serverName, deploymentUnit.getName()))
                .orElse(new SimpleSessionIdentifierCodecBuilder(codecServiceName, serverName));

        Stream.of(factoryBuilder, codecBuilder).forEach(builder -> builder.configure(support).build(target).setInitialMode(Mode.ON_DEMAND).install());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
