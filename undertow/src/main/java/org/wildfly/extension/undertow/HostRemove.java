/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.deployment.DefaultDeploymentMappingProvider;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostRemove extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress parent = address.getParent();
        final String name = address.getLastElement().getValue();
        final String serverName = parent.getLastElement().getValue();
        final ServiceName virtualHostServiceName = HostDefinition.HOST_CAPABILITY.getCapabilityServiceName(serverName, name);
        context.removeService(virtualHostServiceName);
        final ServiceName consoleRedirectName = UndertowService.consoleRedirectServiceName(serverName, name);
        context.removeService(consoleRedirectName);
        final ServiceName commonHostName = WebHost.SERVICE_NAME.append(name);
        context.removeService(commonHostName);
        final String defaultWebModule = HostDefinition.DEFAULT_WEB_MODULE.resolveModelAttribute(context, model).asString();
        DefaultDeploymentMappingProvider.instance().removeMapping(defaultWebModule);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            HostAdd.INSTANCE.performRuntime(context, operation, model);
        } else {
            context.revertReloadRequired();
        }
    }
}
