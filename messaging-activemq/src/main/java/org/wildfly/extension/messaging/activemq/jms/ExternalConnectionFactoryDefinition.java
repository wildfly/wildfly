/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.AbstractTransportDefinition.CONNECTOR_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.AbstractTransportDefinition;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.External;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Regular;

/**
 * JMS Connection Factory resource definition without a broker.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalConnectionFactoryDefinition extends PersistentResourceDefinition {

    private static StringListAttributeDefinition CONNECTORS = new StringListAttributeDefinition.Builder(Common.CONNECTORS)
            .setCapabilityReference(new AbstractTransportDefinition.TransportCapabilityReferenceRecorder("org.wildfly.messaging.activemq.external.connection-factory", CONNECTOR_CAPABILITY_NAME, true))
            .build();
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.external.connection-factory", true, ExternalConnectionFactoryService.class).
            build();
    public static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{
        CommonAttributes.HA, Regular.FACTORY_TYPE, Common.DISCOVERY_GROUP, CONNECTORS, Common.ENTRIES, External.ENABLE_AMQ1_PREFIX, Common.USE_TOPOLOGY};

    private final boolean registerRuntimeOnly;

    public ExternalConnectionFactoryDefinition(final boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.CONNECTION_FACTORY_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTION_FACTORY))
                .setCapabilities(CAPABILITY)
                .setAddHandler(ExternalConnectionFactoryAdd.INSTANCE)
                .setRemoveHandler(ExternalConnectionFactoryRemove.INSTANCE));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            ConnectionFactoryUpdateJndiHandler.registerOperations(registry, getResourceDescriptionResolver());
        }
    }
}
