/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging.ha;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.messaging.CommonAttributes.HA_POLICY;
import static org.jboss.as.messaging.ha.HAAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.ha.HAAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.ha.HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.ha.HAAttributes.RESTART_BACKUP;
import static org.jboss.as.messaging.ha.ManagementHelper.createAddOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hornetq.core.config.HAPolicyConfiguration;
import org.hornetq.core.config.ha.SharedStoreSlavePolicyConfiguration;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.AlternativeAttributeCheckHandler;
import org.jboss.as.messaging.HornetQReloadRequiredHandlers;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class SharedStoreSlaveDefinition extends PersistentResourceDefinition {

    public static Collection<AttributeDefinition> ATTRIBUTES;

    static {
        Collection<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(ALLOW_FAILBACK);
        attributes.add(FAILBACK_DELAY);
        attributes.add(FAILOVER_ON_SERVER_SHUTDOWN);
        attributes.add(RESTART_BACKUP);

        attributes.addAll(ScaleDownAttributes.SCALE_DOWN_ATTRIBUTES);

        ATTRIBUTES = Collections.unmodifiableCollection(attributes);
    }

    private static final AbstractWriteAttributeHandler WRITE_ATTRIBUTE = new HornetQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES) {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new AlternativeAttributeCheckHandler(ATTRIBUTES), MODEL);

            super.execute(context, operation);
        }
    };

    public SharedStoreSlaveDefinition(PathElement path, boolean allowSibling) {
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

    static HAPolicyConfiguration buildConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        return new SharedStoreSlavePolicyConfiguration()
                .setAllowFailBack(ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean())
                .setFailbackDelay(FAILBACK_DELAY.resolveModelAttribute(context, model).asLong())
                .setFailoverOnServerShutdown(FAILOVER_ON_SERVER_SHUTDOWN.resolveModelAttribute(context, model).asBoolean())
                .setRestartBackup(RESTART_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setScaleDownConfiguration(ScaleDownAttributes.addScaleDownConfiguration(context, model));
    }
}