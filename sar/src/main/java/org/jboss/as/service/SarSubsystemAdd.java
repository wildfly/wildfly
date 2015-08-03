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

package org.jboss.as.service;

import javax.management.MBeanServer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.service.component.ServiceComponentProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class SarSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SarSubsystemAdd INSTANCE = new SarSubsystemAdd();

    private SarSubsystemAdd() {
        super(SarExtension.SAR_CAPABILITY);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, ModelNode model) {

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {

                ServiceName jmxCapability = context.getCapabilityServiceName(SarExtension.JMX_CAPABILITY, MBeanServer.class);

                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_SAR_SUB_DEPLOY_CHECK, new SarSubDeploymentProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_SAR, new SarStructureProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SAR_MODULE, new SarModuleDependencyProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_SERVICE_DEPLOYMENT, new ServiceDeploymentParsingProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_SAR_SERVICE_COMPONENT, new ServiceComponentProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_SERVICE_DEPLOYMENT, new ParsedServiceDeploymentProcessor(jmxCapability));
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
