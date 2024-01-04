/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class AccessLogRemove extends AbstractRemoveStepHandler {

    public static final AccessLogRemove INSTANCE = new AccessLogRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final PathAddress hostAddress = context.getCurrentAddress().getParent();
        final PathAddress serverAddress = hostAddress.getParent();
        final String hostName = hostAddress.getLastElement().getValue();
        final String serverName = serverAddress.getLastElement().getValue();
        final ServiceName serviceName = UndertowService.accessLogServiceName(serverName, hostName);
        context.removeService(serviceName);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        AccessLogAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
