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

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.descriptions.ServerGroupDescription;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerGroupAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final ServerGroupAddHandler INSTANCE = new ServerGroupAddHandler();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        final ModelNode subModel = context.getSubModel();
        subModel.get(PROFILE).set(operation.require(PROFILE));

        if(operation.hasDefined(SOCKET_BINDING_GROUP)) {
            subModel.get(SOCKET_BINDING_GROUP).set(operation.get(SOCKET_BINDING_GROUP));
        }

        if(operation.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            subModel.get(SOCKET_BINDING_PORT_OFFSET).set(operation.get(SOCKET_BINDING_PORT_OFFSET));
        }

        if(operation.hasDefined(JVM)) {
            subModel.get(JVM).set(operation.get(JVM).asString(), new ModelNode());
        }
        else {
            subModel.get(JVM);
        }

        subModel.get(DEPLOYMENT);

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.get(OP_ADDR));

        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerGroupDescription.getServerGroupAdd(locale);
    }

}