/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilder;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.session.SimpleDistributableSessionManagerConfiguration;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecBuilder;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor {
    private final String defaultServer;

    public SharedSessionManagerDeploymentProcessor(String defaultServer) {
        this.defaultServer = defaultServer;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(UndertowAttachments.SHARED_SESSION_MANAGER_CONFIG);
        if (sharedConfig == null) {
            return;
        }
        CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        ServiceTarget target = phaseContext.getServiceTarget();
        ServiceName deploymentServiceName = deploymentUnit.getServiceName();
        ServiceName managerServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        if (DistributableSessionManagerFactoryBuilder.INSTANCE.isPresent()) {
            Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            DistributableSessionManagerFactoryBuilder builder = DistributableSessionManagerFactoryBuilder.INSTANCE.get();
            builder.build(target, managerServiceName, new SimpleDistributableSessionManagerConfiguration(sharedConfig, this.defaultServer, deploymentUnit.getName(), module))
                    .setInitialMode(Mode.ON_DEMAND)
                    .install();
        } else {
            InMemorySessionManager manager = new InMemorySessionManager(deploymentUnit.getName(), sharedConfig.getMaxActiveSessions());
            if (sharedConfig.getSessionConfig() != null) {
                if (sharedConfig.getSessionConfig().getSessionTimeoutSet()) {
                    manager.setDefaultSessionTimeout(sharedConfig.getSessionConfig().getSessionTimeout());
                }
            }
            SessionManagerFactory factory = new ImmediateSessionManagerFactory(manager);
            target.addService(managerServiceName, new ValueService<>(new ImmediateValue<>(factory))).setInitialMode(Mode.ON_DEMAND).install();
        }

        ServiceName codecServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME);
        if (DistributableSessionIdentifierCodecBuilder.INSTANCE.isPresent()) {
            DistributableSessionIdentifierCodecBuilder builder = DistributableSessionIdentifierCodecBuilder.INSTANCE.get();
            builder.build(target, codecServiceName, capabilitySupport, this.defaultServer, deploymentUnit.getName()).setInitialMode(Mode.ON_DEMAND).install();
        } else {
            // Fallback to simple codec if server does not support clustering
            new SimpleSessionIdentifierCodecBuilder(codecServiceName, capabilitySupport, this.defaultServer).build(target).setInitialMode(Mode.ON_DEMAND).install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
