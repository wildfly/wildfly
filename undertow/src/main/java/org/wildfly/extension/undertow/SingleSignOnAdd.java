/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import io.undertow.security.impl.SingleSignOnManager;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManagerService;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2014 Red Hat Inc.
 */
class SingleSignOnAdd extends AbstractAddStepHandler {
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : SingleSignOnDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                final PathAddress hostAddress = address.subAddress(0, address.size() - 1);
                final PathAddress serverAddress = hostAddress.subAddress(0, hostAddress.size() - 1);

        final String domain = SingleSignOnDefinition.DOMAIN.resolveModelAttribute(context, model).asString();

        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();
        final ServiceName serviceName = UndertowService.ssoServiceName(serverName, hostName);
        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, hostName);

        final ServiceTarget target = context.getServiceTarget();

        ServiceName managerServiceName = serviceName.append("manager");
        ServiceController<?> factoryController = SingleSignOnManagerService.build(target, managerServiceName, virtualHostServiceName).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        if (newControllers != null) {
            newControllers.add(factoryController);
        }

        final SingleSignOnService service = new SingleSignOnService(domain);
        final ServiceController<?> sc = target.addService(serviceName, service)
                .addDependency(virtualHostServiceName, Host.class, service.getHost())
                .addDependency(managerServiceName, SingleSignOnManager.class, service.getSingleSignOnSessionManager())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        if (newControllers != null) {
            newControllers.add(sc);
        }
    }
}
