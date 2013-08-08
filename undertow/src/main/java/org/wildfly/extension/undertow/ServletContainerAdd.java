/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
final class ServletContainerAdd extends AbstractBoottimeAddStepHandler {
    static final ServletContainerAdd INSTANCE = new ServletContainerAdd();

    ServletContainerAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : ServletContainerDefinition.INSTANCE.getAttributes()) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        installRuntimeServices(context, model, newControllers, name);

    }

    public void installRuntimeServices(OperationContext context, ModelNode model, List<ServiceController<?>> newControllers, String name) throws OperationFailedException {
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        SessionCookieConfig config = SessionCookieDefinition.INSTANCE.getConfig(context, fullModel.get(SessionCookieDefinition.INSTANCE.getPathElement().getKeyValuePair()));

        final boolean developmentMode = ServletContainerDefinition.DEVELOPMENT_MODE.resolveModelAttribute(context, model).asBoolean();
        final boolean allowNonStandardWrappers = ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS.resolveModelAttribute(context, model).asBoolean();

        JSPConfig jspConfig = JspDefinition.INSTANCE.getConfig(context, fullModel.get(JspDefinition.INSTANCE.getPathElement().getKeyValuePair()), developmentMode);

        final ServletContainerService container = new ServletContainerService(developmentMode, allowNonStandardWrappers, config, jspConfig);
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(UndertowService.SERVLET_CONTAINER.append(name), container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
    }
}
