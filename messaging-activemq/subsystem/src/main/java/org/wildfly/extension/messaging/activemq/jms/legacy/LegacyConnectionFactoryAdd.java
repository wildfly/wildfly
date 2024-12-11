/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.DISCOVERY_GROUP;

import jakarta.jms.ConnectionFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryAdd extends AbstractAddStepHandler {

    static final LegacyConnectionFactoryAdd INSTANCE = new LegacyConnectionFactoryAdd();

    public LegacyConnectionFactoryAdd() {
        super(LegacyConnectionFactoryDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        final ServiceName activeMQServerServiceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        LegacyConnectionFactory factory = HornetQHelper.getLegacyConnectionFactory();
        ConnectionFactory incompleteCF = factory.createLegacyConnectionFactory(context, model);
        ModelNode discoveryGroup = DISCOVERY_GROUP.resolveModelAttribute(context, model);
        String discoveryGroupName = discoveryGroup.isDefined() ? discoveryGroup.asString() : null;

        LegacyConnectionFactoryService service = LegacyConnectionFactoryService.installService(name, activeMQServerServiceName, context.getServiceTarget(), factory, incompleteCF, discoveryGroupName, CONNECTORS.unwrap(context, model));
        for (String legacyEntry : LegacyConnectionFactoryDefinition.ENTRIES.unwrap(context, model)) {
            BinderServiceUtil.installBinderService(context.getServiceTarget(), legacyEntry, service, null);
        }
    }

}
