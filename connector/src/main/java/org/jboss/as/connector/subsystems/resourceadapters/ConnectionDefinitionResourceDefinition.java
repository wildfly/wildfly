/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;

import org.jboss.as.connector.subsystems.common.pool.PoolOperations;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConnectionDefinitionResourceDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOLVER = ResourceAdaptersExtension.getResourceDescriptionResolver(CONNECTIONDEFINITIONS_NAME);
    private static final OperationDefinition FLUSH__IDLE_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.FLUSH_IDLE_CONNECTION_IN_POOL, RESOLVER)
            .withFlag(Flag.RUNTIME_ONLY)
            .build();
    private static final OperationDefinition FLUSH_ALL_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.FLUSH_ALL_CONNECTION_IN_POOL, RESOLVER)
            .withFlag(Flag.RUNTIME_ONLY)
            .build();
    private static final OperationDefinition TEST_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.TEST_CONNECTION_IN_POOL, RESOLVER)
            .withFlag(Flag.RUNTIME_ONLY)
            .build();

    private final boolean readOnly;
    private final boolean runtimeOnlyRegistrationValid;

    public ConnectionDefinitionResourceDefinition(final boolean readOnly, final boolean runtimeOnlyRegistrationValid) {
        super(PathElement.pathElement(CONNECTIONDEFINITIONS_NAME), ResourceAdaptersExtension.getResourceDescriptionResolver(CONNECTIONDEFINITIONS_NAME), ConnectionDefinitionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        this.readOnly = readOnly;
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler writeAttributeHandler = new  ReloadRequiredWriteAttributeHandler();
        for (final AttributeDefinition attribute : ResourceAdaptersSubsystemProviders.CONNECTIONDEFINITIONS_NODEATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttributeHandler);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (runtimeOnlyRegistrationValid) {
            resourceRegistration.registerOperationHandler(FLUSH__IDLE_DEFINITION, PoolOperations.FlushIdleConnectionInPool.RA_INSTANCE);
            resourceRegistration.registerOperationHandler(FLUSH_ALL_DEFINITION, PoolOperations.FlushAllConnectionInPool.RA_INSTANCE);
            resourceRegistration.registerOperationHandler(TEST_DEFINITION, PoolOperations.TestConnectionInPool.RA_INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(CDConfigPropertyAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE));
    }
}
