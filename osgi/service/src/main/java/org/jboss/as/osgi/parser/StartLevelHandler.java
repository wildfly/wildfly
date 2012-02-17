/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.Services;
import org.osgi.service.startlevel.StartLevel;

/**
 * Handles the framework start level.
 *
 * @author David Bosschaert
 */
abstract class StartLevelHandler implements OperationStepHandler {
    static final StartLevelHandler READ_HANDLER = new StartLevelHandler() {
        @Override
        void invokeOperation(StartLevel sls, OperationContext context, ModelNode operation) {
            int sl = sls.getStartLevel();
            context.getResult().set(sl);
        }
    };

    static final StartLevelHandler WRITE_HANDLER = new StartLevelHandler() {
        @Override
        void invokeOperation(StartLevel sls, OperationContext context, ModelNode operation) {
            int targetStartLevel = operation.require(ModelDescriptionConstants.VALUE).asInt();
            sls.setStartLevel(targetStartLevel);
        }
    };

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ServiceController<?> svc = context.getServiceRegistry(false).getRequiredService(Services.START_LEVEL);

        if (svc == null || svc.getState() != ServiceController.State.UP) {
            // non-metric read-attribute handlers should not fail
            // OSGiMessages.MESSAGES.osgiSubsystemNotActive()
            context.getResult().set(new ModelNode());
        } else {
            StartLevel sls = (StartLevel) svc.getValue();
            invokeOperation(sls, context, operation);
        }

        context.completeStep();
    }

    abstract void invokeOperation(StartLevel sls, OperationContext context, ModelNode operation);
}
