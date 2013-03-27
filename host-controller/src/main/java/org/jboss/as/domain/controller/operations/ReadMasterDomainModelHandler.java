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

package org.jboss.as.domain.controller.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Step handler responsible for collecting a complete description of the domain model,
 * which is going to be sent back to a remote host-controller. This is called when the
 * remote slave boots up or when it reconnects to the DC
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public class ReadMasterDomainModelHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "read-master-domain-model";

    protected final String host;
    protected final Transformers transformers;
    protected final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry;

    public ReadMasterDomainModelHandler(final String host, final Transformers transformers, DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry) {
        this.host = host;
        this.transformers = transformers;
        this.runtimeIgnoreTransformationRegistry = runtimeIgnoreTransformationRegistry;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Acquire the lock to make sure that nobody can modify the model before the slave has applied it
        context.acquireControllerLock();

        final Resource rootResource = context.readResource(PathAddress.EMPTY_ADDRESS,true);
        final ReadMasterDomainModelUtil readUtil = ReadMasterDomainModelUtil.readMasterDomainResourcesForInitialConnect(context, transformers, rootResource, runtimeIgnoreTransformationRegistry);
        context.getResult().set(readUtil.getDescribedResources());

        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                runtimeIgnoreTransformationRegistry.addKnownDataForSlave(host, readUtil.getNewKnownRootResources());
            }
        });
    }
}
