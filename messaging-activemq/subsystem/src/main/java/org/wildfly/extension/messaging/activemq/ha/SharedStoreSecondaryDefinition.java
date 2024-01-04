/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.ha;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.ALLOW_FAILBACK;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.RESTART_BACKUP;
import static org.wildfly.extension.messaging.activemq.ha.ManagementHelper.createAddOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class SharedStoreSecondaryDefinition extends PersistentResourceDefinition {

    public static final Collection<AttributeDefinition> ATTRIBUTES;

    static {
        Collection<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(ALLOW_FAILBACK);
        attributes.add(FAILOVER_ON_SERVER_SHUTDOWN);
        attributes.add(RESTART_BACKUP);

        attributes.addAll(ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES);

        ATTRIBUTES = Collections.unmodifiableCollection(attributes);
    }

    private static final AbstractWriteAttributeHandler WRITE_ATTRIBUTE = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);

    public SharedStoreSecondaryDefinition(PathElement path, boolean allowSibling) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                createAddOperation(path.getKey(), allowSibling, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, WRITE_ATTRIBUTE);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

}