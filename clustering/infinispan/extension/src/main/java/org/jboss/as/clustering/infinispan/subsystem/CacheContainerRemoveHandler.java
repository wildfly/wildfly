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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Remove a cache container, taking care to remove any child cache resources as well.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat, Inc.
 */
public class CacheContainerRemoveHandler extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();

        // remove any existing cache entries
        for (CacheType type: CacheType.values()) {
            CacheAddHandler addHandler = type.getAddHandler();
            if (model.hasDefined(type.pathElement().getKey())) {
                for (Property property: model.get(type.pathElement().getKey()).asPropertyList()) {
                    ModelNode removeOperation = Util.createRemoveOperation(address.append(type.pathElement(property.getName())));
                    addHandler.removeRuntimeServices(context, removeOperation, model, property.getValue());
                }
            }
        }

        CacheContainerAddHandler.removeRuntimeServices(context, operation, model);
    }

    /**
     * Method to re-install any services associated with existing local caches.
     *
     * @param context
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = context.getCurrentAddress();

        // re-install the cache container services
        CacheContainerAddHandler.installRuntimeServices(context, operation, model);

        // re-install any existing cache services
        for (CacheType type: CacheType.values()) {
            CacheAddHandler addHandler = type.getAddHandler();
            if (model.hasDefined(type.pathElement().getKey())) {
                for (Property property: model.get(type.pathElement().getKey()).asPropertyList()) {
                    ModelNode addOperation = Util.createAddOperation(address.append(type.pathElement(property.getName())));
                    addHandler.installRuntimeServices(context, addOperation, model, property.getValue());
                }
            }
        }
    }
}
