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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Paul Ferraro
 */
public class CacheContainerRemove implements ModelRemoveOperationHandler, DescriptionProvider {

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getCacheContainerRemoveDescription(locale);
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        ModelNode opAddr = operation.require(ModelDescriptionConstants.OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        ModelNode restoreOperation = CacheContainerAdd.createOperation(opAddr, context.getSubModel());

        RuntimeOperationContext runtime = context.getRuntimeContext();
        if (runtime != null) {
            RuntimeTask task = new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> service = context.getServiceRegistry().getService(EmbeddedCacheManagerService.getServiceName(name));
                    if (service != null) {
                        service.addListener(new ResultHandler.ServiceRemoveListener(resultHandler));
                    } else {
                        resultHandler.handleResultComplete();
                    }
                }
            };
            runtime.setRuntimeTask(task);
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(restoreOperation);
    }
}
