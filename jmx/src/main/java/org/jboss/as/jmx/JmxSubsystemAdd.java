/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author Emanuel Muckenhuber
 */
class JMXSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final JMXSubsystemAdd INSTANCE = new JMXSubsystemAdd();

    private JMXSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        if(context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext bootContext = (RuntimeOperationContext) context;
            // Add the MBean service
            MBeanServerService.addService(bootContext.getServiceTarget());
        }

        context.getSubModel().get(CommonAttributes.SERVER_BINDING);
        context.getSubModel().get(CommonAttributes.REGISTRY_BINDING);

        // TODO add a remove handler
        resultHandler.handleResultComplete(Util.getResourceRemoveOperation(operation.require(OP_ADDR)));

        return Cancellable.NULL;
    }

}
