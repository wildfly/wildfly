/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for adding the mail subsystem resource to the model
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final MailSubsystemAdd INSTANCE = new MailSubsystemAdd();

    private MailSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MailExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_MAIL_SESSION, new MailSessionDefinitionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(MailExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MAIL, new MailDependenciesProcessor());
                processorTarget.addDeploymentProcessor(MailExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_MAIL_SESSION, new MailSessionDefinitionDescriptorProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
