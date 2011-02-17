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

package org.jboss.as.arquillian.parser;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.arquillian.service.ArquillianDeploymentProcessor;
import org.jboss.as.arquillian.service.ArquillianRunWithAnnotationProcessor;
import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.as.arquillian.service.ArquillianDependencyProcessor;
import org.jboss.logging.Logger;

/**
 * Arquillian subsystem add handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @author Emanuel Muckenhuber
 */
class ArquillianSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");
    static final ArquillianSubsystemAdd INSTANCE = new ArquillianSubsystemAdd();

    private ArquillianSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));
        context.getSubModel().setEmptyObject();

        if(context instanceof BootOperationContext) {
            log.infof("Activating Arquillian Subsystem");
                final BootOperationContext bootContext = (BootOperationContext) context;
                ArquillianService.addService(context.getRuntimeContext().getServiceTarget());
                bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ARQUILLIAN_RUNWITH, new ArquillianRunWithAnnotationProcessor());
                bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_ARQUILLIAN_DEPLOYMENT, new ArquillianDeploymentProcessor());
                bootContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_ARQUILLIAN,
                        new ArquillianDependencyProcessor());
                resultHandler.handleResultComplete();
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

}
