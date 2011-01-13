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

package org.jboss.as.ee.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.ee.processor.EarStructureProcessor;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Emanuel Muckenhuber
 */
public class NewEeSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    private static final Logger logger = Logger.getLogger("org.jboss.as.ee");

    static final NewEeSubsystemAdd INSTANCE = new NewEeSubsystemAdd();

    private NewEeSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        if(context instanceof NewBootOperationContext) {
            final NewBootOperationContext bootContext = (NewBootOperationContext) context;
            logger.info("Activating EE subsystem");
            bootContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT, new EarStructureProcessor());
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove");
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
