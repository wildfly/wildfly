/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Read handler for deployed Jakarta Messaging pooled connection factories
 *
 * @author Stuart Douglas
 */
public class PooledConnectionFactoryConfigurationRuntimeHandler extends AbstractJMSRuntimeHandler<ModelNode> {

    public static final PooledConnectionFactoryConfigurationRuntimeHandler INSTANCE = new PooledConnectionFactoryConfigurationRuntimeHandler(false);
    public static final PooledConnectionFactoryConfigurationRuntimeHandler EXTERNAL_INSTANCE = new PooledConnectionFactoryConfigurationRuntimeHandler(true);

    private final boolean external;

    private PooledConnectionFactoryConfigurationRuntimeHandler(final boolean external) {
        this.external = external;
    }

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final ModelNode connectionFactory, final PathAddress address, final boolean includeDefault) {
        if (connectionFactory.hasDefined(attributeName)) {
            context.getResult().set(connectionFactory.get(attributeName));
        } else {
            ConnectionFactoryAttribute attribute = external ? ExternalPooledConnectionFactoryDefinition.getAttributesMap().get(attributeName) : PooledConnectionFactoryDefinition.getAttributesMap().get(attributeName);
            if (attribute != null && attribute.getDefinition().getDefaultValue() != null && attribute.getDefinition().getDefaultValue().isDefined()) {
                context.getResult().set(attribute.getDefinition().getDefaultValue());
            }
        }
    }

}
