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

package org.wildfly.extension.classchange;


import java.util.ServiceLoader;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

/**
 * The EE subsystem add update handler.
 *
 * @author Stuart Douglas
 */
class ClassChangeSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final ClassChangeSubsystemAdd INSTANCE = new ClassChangeSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        if (context.getProcessType().isManagedDomain()) {
            throw ClassChangeMessages.ROOT_LOGGER.domainModeNotSupported();
        }
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                //we only add hot deployment processor if Fakereplace is installed
                //which is why this is hidden behind a service loader
                try {
                    ServiceLoader<ClassChangeSupportInstaller> loader = ServiceLoader.load(ClassChangeSupportInstaller.class);
                    for (ClassChangeSupportInstaller installer : loader) {
                        installer.install();
                    }
                } catch (Throwable e) {
                    //ignore
                    ClassChangeMessages.ROOT_LOGGER.debug("Not installing hot deployment support as Fakereplace is not present", e);
                }


            }
        }, OperationContext.Stage.RUNTIME);
    }
}
