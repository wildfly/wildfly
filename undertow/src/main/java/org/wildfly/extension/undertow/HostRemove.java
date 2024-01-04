/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        if (context.isResourceServiceRestartAllowed()) {
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
        } else {
            context.reloadRequired();
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            HostAdd.INSTANCE.performRuntime(context, operation, model);
        } else {
            context.revertReloadRequired();
        }
    }
}
