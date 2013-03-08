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
package org.jboss.as.osgi.management;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.parser.OSGiRootResource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Handles the framework start level.
 *
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public abstract class StartLevelHandler implements OperationStepHandler {

    public static final StartLevelHandler READ_HANDLER = new StartLevelHandler() {
        @Override
        void invokeOperation(Bundle sysbundle, OperationContext context, ModelNode operation) {
            FrameworkStartLevel frameworkStartLevel = sysbundle.adapt(FrameworkStartLevel.class);
            int startlevel = frameworkStartLevel.getStartLevel();
            context.getResult().set(startlevel);
        }
    };

    public static final StartLevelHandler WRITE_HANDLER = new StartLevelHandler() {
        @Override
        void invokeOperation(Bundle sysbundle, OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode value = operation.require(ModelDescriptionConstants.VALUE);
            OSGiRootResource.STARTLEVEL.getValidator().validateParameter(ModelDescriptionConstants.VALUE, value);
            int startlevel = value.asInt();
            FrameworkStartLevel frameworkStartLevel = sysbundle.adapt(FrameworkStartLevel.class);
            frameworkStartLevel.setStartLevel(startlevel);
        }
    };

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ServiceController<?> controller = context.getServiceRegistry(false).getService(Services.SYSTEM_BUNDLE);
        if (controller != null && controller.getState() == ServiceController.State.UP) {
            Bundle sysbundle =  (Bundle) controller.getValue();
            invokeOperation(sysbundle, context, operation);
        } else {
            // non-metric read-attribute handlers should not fail
            context.getResult().set(new ModelNode());
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    abstract void invokeOperation(Bundle sysbundle, OperationContext context, ModelNode operation) throws OperationFailedException ;
}
