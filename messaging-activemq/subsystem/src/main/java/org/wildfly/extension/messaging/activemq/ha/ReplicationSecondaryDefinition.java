/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.ha;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.ALLOW_FAILBACK;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CLUSTER_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.GROUP_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
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
public class ReplicationSecondaryDefinition extends PersistentResourceDefinition {

    public static final Collection<AttributeDefinition> ATTRIBUTES;

    static {
        Collection<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(CLUSTER_NAME);
        attributes.add(GROUP_NAME);
        attributes.add(ALLOW_FAILBACK);
        attributes.add(INITIAL_REPLICATION_SYNC_TIMEOUT);
        attributes.add(MAX_SAVED_REPLICATED_JOURNAL_SIZE);
        attributes.add(RESTART_BACKUP);

        attributes.addAll(ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES);

        ATTRIBUTES = Collections.unmodifiableCollection(attributes);
    }

    private final boolean registerRuntime;
    public ReplicationSecondaryDefinition(PathElement path, boolean allowSibling, boolean registerRuntime) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                createAddOperation(path.getKey(), allowSibling, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntime = registerRuntime;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AbstractWriteAttributeHandler writeAttribute = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttribute);
        }
        if(registerRuntime) {
            HAPolicySynchronizationStatusReadHandler.registerSlaveAttributes(resourceRegistration);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

}