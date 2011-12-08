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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class JMXConnectorRemove extends AbstractRemoveStepHandler {

    static final JMXConnectorRemove INSTANCE = new JMXConnectorRemove();

    static final String OPERATION_NAME = "remove-connector";

    private JMXConnectorRemove() {
        //
    }

    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) {
        final ModelNode toUpdate = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        toUpdate.get(CommonAttributes.SERVER_BINDING).clear();
        toUpdate.get(CommonAttributes.REGISTRY_BINDING).clear();
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.removeService(JMXConnectorService.SERVICE_NAME);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        JMXConnectorAdd.INSTANCE.launchServices(context, model, null, null);
    }
}
