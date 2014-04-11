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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(UndertowAttachments.SHARED_SESSION_MANAGER_CONFIG);
        if(sharedConfig == null) {
            return;
        }
        ServiceName serviceName = deploymentUnit.getServiceName().append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        if(sharedConfig.getReplicationConfig() == null) {
            InMemorySessionManager manager = new InMemorySessionManager(deploymentUnit.getName(), sharedConfig.getMaxActiveSessions());
            if(sharedConfig.getSessionConfig() != null) {
                if(sharedConfig.getSessionConfig().getSessionTimeoutSet()) {
                    manager.setDefaultSessionTimeout(sharedConfig.getSessionConfig().getSessionTimeout());
                }
            }
            phaseContext.getServiceTarget().addService(serviceName, new ValueService<SessionManagerFactory>(new ImmediateValue<SessionManagerFactory>(new ImmediateSessionManagerFactory(manager))))
                    .install();
        } else {
            throw new DeploymentUnitProcessingException("Clustering not supported yet in shared session managers");
        }


    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
