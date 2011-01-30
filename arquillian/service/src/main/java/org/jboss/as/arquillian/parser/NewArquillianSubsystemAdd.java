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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.arquillian.service.ArquillianDeploymentProcessor;
import org.jboss.as.arquillian.service.ArquillianRunWithAnnotationProcessor;
import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class NewArquillianSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final NewArquillianSubsystemAdd INSTANCE = new NewArquillianSubsystemAdd();

    private NewArquillianSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(context instanceof NewBootOperationContext) {
            final NewBootOperationContext bootContext = (NewBootOperationContext) context;
            ArquillianService.addService(bootContext.getServiceTarget());
            bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ARQUILLIAN_RUNWITH, new ArquillianRunWithAnnotationProcessor());
            bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_ARQUILLIAN_DEPLOYMENT, new ArquillianDeploymentProcessor());
        }

        context.getSubModel().setEmptyObject();

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
