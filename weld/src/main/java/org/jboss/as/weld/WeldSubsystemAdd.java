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

package org.jboss.as.weld;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.deployment.processors.BeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.BeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.WebIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldBeanManagerServiceProcessor;
import org.jboss.as.weld.deployment.processors.WeldComponentIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor;
import org.jboss.as.weld.deployment.processors.WeldEjbInterceptorIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldPortableExtensionProcessor;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * The weld subsystem add update handler.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 */
class WeldSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final WeldSubsystemAdd INSTANCE = new WeldSubsystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        context.getSubModel().setEmptyObject();

        if (context instanceof BootOperationContext) {
            final BootOperationContext bootContext = (BootOperationContext) context;
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    bootContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
                    bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new BeansXmlProcessor());
                    bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_COMPONENT_INTEGRATION, new WeldComponentIntegrationProcessor());
                    bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
                    bootContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_EJB_INTERCEPTORS_INTEGRATION, new WeldEjbInterceptorIntegrationProcessor());
                    bootContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_BEAN_ARCHIVE, new BeanArchiveProcessor());
                    bootContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS, new WeldPortableExtensionProcessor());
                    bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor());
                    bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new WeldBeanManagerServiceProcessor());

                    TCCLSingletonService singleton = new TCCLSingletonService();
                    context.getServiceTarget().addService(TCCLSingletonService.SERVICE_NAME, singleton).setInitialMode(
                            Mode.ON_DEMAND).install();
                    resultHandler.handleResultComplete();
                }
            });
            CurrentServiceRegistry.setServiceRegistry(bootContext.getController().getServiceRegistry());
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));
        return new BasicOperationResult(compensatingOperation);
    }

}
