/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * The add operation for the messaging subsystem's http-acceptor resource.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPAcceptorAdd extends ActiveMQReloadRequiredHandlers.AddStepHandler {

    public static final HTTPAcceptorAdd INSTANCE = new HTTPAcceptorAdd();

    private HTTPAcceptorAdd() {
        super(HTTPAcceptorDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);

        String acceptorName = context.getCurrentAddressValue();
        String activeMQServerName = context.getCurrentAddress().getParent().getLastElement().getValue();
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        launchServices(context, activeMQServerName, acceptorName, fullModel);
    }

    void launchServices(OperationContext context, String activeMQServerName, String acceptorName, ModelNode model) throws OperationFailedException {
        String httpConnectorName = HTTPAcceptorDefinition.HTTP_LISTENER.resolveModelAttribute(context, model).asString();

        HTTPUpgradeService.installService(context.getCapabilityServiceTarget(),
                activeMQServerName,
                acceptorName,
                httpConnectorName);

        boolean upgradeLegacy = HTTPAcceptorDefinition.UPGRADE_LEGACY.resolveModelAttribute(context, model).asBoolean();
        if (upgradeLegacy) {
            HTTPUpgradeService.LegacyHttpUpgradeService.installService(context,
                    activeMQServerName,
                    acceptorName,
                    httpConnectorName);
        }
    }
}
