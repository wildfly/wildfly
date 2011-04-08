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
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Paul Ferraro
 */
public class InfinispanSubsystemAdd implements ModelAddOperationHandler, DescriptionProvider {
    private static final Logger log = Logger.getLogger(InfinispanSubsystemAdd.class.getPackage().getName());

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        if (source.hasDefined(ModelKeys.DEFAULT_CONTAINER)) {
            target.get(ModelKeys.DEFAULT_CONTAINER).set(source.get(ModelKeys.DEFAULT_CONTAINER));
        }
        target.get(ModelKeys.CACHE_CONTAINER).setEmptyObject();
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getSubsystemAddDescription(locale);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.ModelAddOperationHandler#execute(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.as.controller.ResultHandler)
     */
    @Override
    public OperationResult execute(OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        log.info("Activating Infinispan subsystem.");

        populate(operation, context.getSubModel());

        RuntimeOperationContext runtime = context.getRuntimeContext();
        if (runtime != null) {
            RuntimeTask task = new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    CacheContainerRegistryService service = new CacheContainerRegistryService();
                    if (operation.hasDefined(ModelKeys.DEFAULT_CONTAINER)) {
                        service.setDefaultContainer(operation.get(ModelKeys.DEFAULT_CONTAINER).asString());
                    }
                    service.build(context.getServiceTarget())
                        .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                        .install();
                }
            };

            runtime.setRuntimeTask(task);
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(Util.getResourceRemoveOperation(operation.require(ModelDescriptionConstants.OP_ADDR)));
    }
}
