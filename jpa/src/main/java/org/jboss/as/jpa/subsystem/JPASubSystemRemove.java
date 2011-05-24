/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Removes the JPA subsystem.
 * <p/>
 *
 * @author Brian Stansberry
 */
class JPASubSystemRemove implements ModelRemoveOperationHandler, BootOperationHandler, DescriptionProvider {

    static final JPASubSystemRemove INSTANCE = new JPASubSystemRemove();

    static final String OPERATION_NAME = REMOVE;

    private JPASubSystemRemove() {
        //
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
        throws OperationFailedException {

        ModelNode compensatingOperation = JPASubSystemAdd.getAddOperation(operation.require(OP_ADDR), context.getSubModel());
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry registry = context.getServiceRegistry();
                    ServiceController<?> jpaService = registry.getService(JPAService.SERVICE_NAME);
                    if (jpaService != null) {
                        jpaService.setMode(Mode.REMOVE);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return JPADescriptions.getSubsystemRemove(locale);
    }

}
