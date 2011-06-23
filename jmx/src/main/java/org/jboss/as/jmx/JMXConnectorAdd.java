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

import java.util.List;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class JMXConnectorAdd extends AbstractAddStepHandler {

    static final JMXConnectorAdd INSTANCE = new JMXConnectorAdd();

    static final String OPERATION_NAME = "add-connector";

    private JMXConnectorAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String serverBinding = operation.require(CommonAttributes.SERVER_BINDING).asString();
        final String registryBinding = operation.require(CommonAttributes.REGISTRY_BINDING).asString();

        model.get(CommonAttributes.SERVER_BINDING).set(serverBinding);
        model.get(CommonAttributes.REGISTRY_BINDING).set(registryBinding);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String serverBinding = operation.require(CommonAttributes.SERVER_BINDING).asString();
        final String registryBinding = operation.require(CommonAttributes.REGISTRY_BINDING).asString();
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(JMXConnectorService.addService(target, serverBinding, registryBinding, verificationHandler));
    }
}
