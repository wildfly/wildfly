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

package org.wildfly.extension.picketlink.federation.model.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.picketlink.config.federation.KeyValueType;
import org.wildfly.extension.picketlink.federation.service.EntityProviderService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class HandlerParameterAddHandler extends AbstractAddStepHandler {

    static final HandlerParameterAddHandler INSTANCE = new HandlerParameterAddHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : HandlerParameterResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ADDRESS));
        String providerAlias = pathAddress.subAddress(0, pathAddress.size() - 2).getLastElement().getValue();
        String handlerType = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        EntityProviderService providerService = EntityProviderService.getService(context, providerAlias);
        String handlerParameterName = pathAddress.getLastElement().getValue();
        KeyValueType keyValueType = toHandlerParameterConfig(context, handlerParameterName, model);

        providerService.addHandlerParameter(handlerType, keyValueType);
    }

    public static KeyValueType toHandlerParameterConfig(OperationContext context, String paramName, ModelNode parameterNode) throws OperationFailedException {
        String paramValue = HandlerParameterResourceDefinition.VALUE
            .resolveModelAttribute(context, parameterNode).asString();

        KeyValueType kv = new KeyValueType();

        kv.setKey(paramName);
        kv.setValue(paramValue);
        return kv;
    }

    @Override protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            HandlerParameterRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {

        }
    }
}
