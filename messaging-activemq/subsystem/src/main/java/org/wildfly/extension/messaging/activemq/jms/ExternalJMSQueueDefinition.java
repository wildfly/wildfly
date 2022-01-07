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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.External;

/**
 * Jakarta Messaging Queue resource definition
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSQueueDefinition extends PersistentResourceDefinition {

    public static final AttributeDefinition[] ATTRIBUTES = {CommonAttributes.DESTINATION_ENTRIES, External.ENABLE_AMQ1_PREFIX};
    private final boolean registerRuntimeOnly;

    public ExternalJMSQueueDefinition(boolean registerRuntimeOnly) {
        super(MessagingExtension.EXTERNAL_JMS_QUEUE_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.EXTERNAL_JMS_QUEUE),
                ExternalJMSQueueAdd.INSTANCE,
                ExternalJMSQueueRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(CommonAttributes.DESTINATION_ENTRIES, External.ENABLE_AMQ1_PREFIX);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            registry.registerReadOnlyAttribute(CommonAttributes.DESTINATION_ENTRIES, null);
            // Should this be read only as entries ?
            registry.registerReadOnlyAttribute(External.ENABLE_AMQ1_PREFIX, null);
        } else {
            registry.registerReadWriteAttribute(CommonAttributes.DESTINATION_ENTRIES, null, new ReloadRequiredWriteAttributeHandler(CommonAttributes.DESTINATION_ENTRIES));
            registry.registerReadWriteAttribute(External.ENABLE_AMQ1_PREFIX, null, new ReloadRequiredWriteAttributeHandler(External.ENABLE_AMQ1_PREFIX));
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.singletonList(MessagingExtension.JMS_QUEUE_ACCESS_CONSTRAINT);
    }
}
