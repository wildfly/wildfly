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

package org.jboss.as.jaxrs;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.jaxrs.deployment.JaxrsAnnotationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsIntegrationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsScanningProcessor;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * The jaxrs subsystem add update handler.
 *
 * @author Stuart Douglas
 */
class JaxrsSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final JaxrsSubsystemAdd INSTANCE = new JaxrsSubsystemAdd();

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
                    bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JAXRS_ANNOTATIONS, new JaxrsAnnotationProcessor());
                    bootContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRS, new JaxrsDependencyProcessor());
                    bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JAXRS_SCANNING, new JaxrsScanningProcessor());
                    bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JAXRS_DEPLOYMENT, new JaxrsIntegrationProcessor());
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));
        return new BasicOperationResult(compensatingOperation);
    }

}
