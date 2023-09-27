/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * The remove operation for the messaging subsystem's http-acceptor resource.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPAcceptorRemove extends AbstractRemoveStepHandler {

    static final HTTPAcceptorRemove INSTANCE = new HTTPAcceptorRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String acceptorName = context.getCurrentAddressValue();
        final String serverName = context.getCurrentAddress().getParent().getLastElement().getValue();
        context.removeService(MessagingServices.getHttpUpgradeServiceName(serverName, acceptorName));

        boolean upgradeLegacy = HTTPAcceptorDefinition.UPGRADE_LEGACY.resolveModelAttribute(context, model).asBoolean();
        if (upgradeLegacy) {
            context.removeService(MessagingServices.getLegacyHttpUpgradeServiceName(serverName, acceptorName));
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String acceptorName = context.getCurrentAddressValue();
        final String serverName = context.getCurrentAddress().getParent().getLastElement().getValue();
        HTTPAcceptorAdd.INSTANCE.launchServices(context, serverName, acceptorName, model);
    }
}
