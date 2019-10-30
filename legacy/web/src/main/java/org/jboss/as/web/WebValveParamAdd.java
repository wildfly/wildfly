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

package org.jboss.as.web;

import static org.jboss.as.web.Constants.PARAM;
import static org.jboss.as.web.WebMessages.MESSAGES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author Jean-Frederic Clere
 */
public class WebValveParamAdd implements OperationStepHandler {

    static final WebValveParamAdd INSTANCE = new WebValveParamAdd();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode mimetypes = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(PARAM);
        if (operation.hasDefined("param-name") && operation.hasDefined("param-value")) {
            mimetypes.get(operation.get("param-name").asString()).set(operation.get("param-value").asString());
        } else {
            throw new OperationFailedException(MESSAGES.paramNameAndParamValueRequiredForAddParam());
        }

        if (!context.isBooting() && context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.reloadRequired();
                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            context.revertReloadRequired();
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
