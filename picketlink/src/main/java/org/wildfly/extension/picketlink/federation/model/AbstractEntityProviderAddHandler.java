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
package org.wildfly.extension.picketlink.federation.model;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.picketlink.config.federation.KeyValueType;
import org.picketlink.config.federation.handler.Handler;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.service.EntityProviderService;

import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerAddHandler.toHandlerConfig;
import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerParameterAddHandler.toHandlerParameterConfig;

/**
 * @author Pedro Igor
 */
public abstract class AbstractEntityProviderAddHandler extends AbstractAddStepHandler {

    protected static void configureHandler(OperationContext context, ModelNode model, EntityProviderService service) throws OperationFailedException {
        if (model.hasDefined(ModelElement.COMMON_HANDLER.getName())) {
            for (Property handlerProperty : model.get(ModelElement.COMMON_HANDLER.getName()).asPropertyList()) {
                ModelNode handler = handlerProperty.getValue();
                Handler newHandler = toHandlerConfig(context, handler);

                if (handler.hasDefined(COMMON_HANDLER_PARAMETER.getName())) {
                    for (Property handlerParameter : handler.get(COMMON_HANDLER_PARAMETER.getName()).asPropertyList()) {
                        String paramName = handlerParameter.getName();
                        ModelNode parameterNode = handlerParameter.getValue();
                        KeyValueType kv = toHandlerParameterConfig(context, paramName, parameterNode);

                        newHandler.add(kv);
                    }
                }

                service.addHandler(newHandler);
            }
        }
    }

}
