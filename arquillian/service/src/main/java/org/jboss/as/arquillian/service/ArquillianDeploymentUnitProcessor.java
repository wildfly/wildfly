/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.arquillian.service;

import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for creating and managing the life-cycle of the Arquillian service.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @version $Revision: 1.1 $
 */
public class ArquillianDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final ServiceName NAME_BASE = ServiceName.JBOSS.append("arquillian", "deployment", "tracker");

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if (context.getAttachment(ArquillianConfig.ATTACHMENT_KEY) == null) {
            return;
        }

        BatchBuilder batchBuilder = context.getBatchBuilder();
        DeploymentTrackerService tracker = new DeploymentTrackerService(context);
        batchBuilder.addService(NAME_BASE.append(context.getName()), tracker)
            .addDependency(ArquillianService.SERVICE_NAME, ArquillianService.class, tracker.injectedArquillianService);
    }

    private class DeploymentTrackerService implements Service<Object>{
        private final DeploymentUnitContext deploymentUnitContext;
        private final InjectedValue<ArquillianService> injectedArquillianService = new InjectedValue<ArquillianService>();

        public DeploymentTrackerService(DeploymentUnitContext deploymentUnitContext) {
            this.deploymentUnitContext = deploymentUnitContext;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ArquillianService service = injectedArquillianService.getValue();
            service.registerDeployment(deploymentUnitContext);
        }

        @Override
        public void stop(StopContext context) {
            ArquillianService service = injectedArquillianService.getValue();
            service.unregisterDeployment(deploymentUnitContext);
            //Remove this just to be sure we don't leak classes
            deploymentUnitContext.removeAttachment(ArquillianConfig.ATTACHMENT_KEY);
        }

        @Override
        public Object getValue() throws IllegalStateException {
            return null;
        }
    }
}
