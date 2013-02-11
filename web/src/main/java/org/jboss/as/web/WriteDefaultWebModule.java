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

import static org.jboss.as.web.WebMessages.MESSAGES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

public class WriteDefaultWebModule implements OperationStepHandler {
    static final WriteDefaultWebModule INSTANCE = new WriteDefaultWebModule();
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode virtualHost = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        String war = operation.get("value").asString();
        if(virtualHost.hasDefined(Constants.ENABLE_WELCOME_ROOT) && virtualHost.get(Constants.ENABLE_WELCOME_ROOT).asBoolean()) {
            // That is no supported.
            throw new OperationFailedException(MESSAGES.noWelcomeWebappWithDefaultWebModule());
        } else {
            virtualHost.get(Constants.DEFAULT_WEB_MODULE).set(war);
        }
        if (context.isNormalServer()) {
            context.reloadRequired();
        }
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (context.isNormalServer()) {
                    context.revertReloadRequired();
                }
            }
        });
    }
}
