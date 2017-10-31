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
import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition.getHandlerType;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.picketlink.config.federation.handler.Handler;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.AlternativeAttributeValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.UniqueTypeValidationStepHandler;
import org.wildfly.extension.picketlink.federation.service.EntityProviderService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class HandlerAddHandler extends AbstractAddStepHandler {

    static final HandlerAddHandler INSTANCE = new HandlerAddHandler();

    private HandlerAddHandler() {

    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new AlternativeAttributeValidationStepHandler(new SimpleAttributeDefinition[] {
            HandlerResourceDefinition.CLASS_NAME, HandlerResourceDefinition.CODE
        }), OperationContext.Stage.MODEL);
        context.addStep(new UniqueTypeValidationStepHandler(ModelElement.COMMON_HANDLER) {
            @Override
            protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                return getHandlerType(context, model);
            }
        }, OperationContext.Stage.MODEL);
        super.execute(context, operation);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : HandlerResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ADDRESS));
        String providerAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        EntityProviderService providerService = EntityProviderService.getService(context, providerAlias);
        Handler handler = toHandlerConfig(context, model);

        providerService.addHandler(handler);
    }

    @Override protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            HandlerRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }

    public static Handler toHandlerConfig(OperationContext context, ModelNode handler) throws OperationFailedException {
        Handler newHandler = new Handler();
        String typeName = HandlerResourceDefinition.getHandlerType(context, handler);

        newHandler.setClazz(typeName);

        return newHandler;
    }
}
