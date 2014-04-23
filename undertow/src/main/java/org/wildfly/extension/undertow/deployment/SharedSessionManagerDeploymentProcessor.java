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
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilderValue;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.session.SimpleDistributableSessionManagerConfiguration;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(UndertowAttachments.SHARED_SESSION_MANAGER_CONFIG);
        if (sharedConfig == null) {
            return;
        }
        ServiceTarget target = phaseContext.getServiceTarget();
        ServiceName deploymentServiceName = deploymentUnit.getServiceName();
        ServiceName serviceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        DistributableSessionManagerFactoryBuilder builder = new DistributableSessionManagerFactoryBuilderValue().getValue();
        if (builder != null) {
            Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            builder.build(target, serviceName, new SimpleDistributableSessionManagerConfiguration(sharedConfig, deploymentUnit.getName(), module))
                    .setInitialMode(Mode.ON_DEMAND)
                    .install()
            ;
        } else {
            InMemorySessionManager manager = new InMemorySessionManager(deploymentUnit.getName(), sharedConfig.getMaxActiveSessions());
            if (sharedConfig.getSessionConfig() != null) {
                if (sharedConfig.getSessionConfig().getSessionTimeoutSet()) {
                    manager.setDefaultSessionTimeout(sharedConfig.getSessionConfig().getSessionTimeout());
                }
            }
            SessionManagerFactory factory = new ImmediateSessionManagerFactory(manager);
            target.addService(serviceName, new ValueService<>(new ImmediateValue<>(factory)))
                    .setInitialMode(Mode.ON_DEMAND)
                    .install()
            ;
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
