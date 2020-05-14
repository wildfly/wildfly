/*
 * Copyright 2019 JBoss by Red Hat.
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
package org.wildfly.extension.messaging.activemq.shallow;


import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public abstract class ShallowResourceDefinition extends PersistentResourceDefinition implements OperationAddressConverter, IgnoredAttributeProvider {

    protected final boolean registerRuntimeOnly;

    public ShallowResourceDefinition(SimpleResourceDefinition.Parameters parameters, boolean registerRuntimeOnly) {
        super(parameters);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        TranslatedOperationHandler handler = new TranslatedOperationHandler(this);
        // Override global operations with transformed operations, if necessary
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_CLEAR_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_PUT_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_GET_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_REMOVE_DEFINITION, handler);

        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_ADD_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_GET_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_REMOVE_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_CLEAR_DEFINITION, handler);
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : getAttributes()) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, new TranslatedReadAttributeHandler(this, this), new TranslatedWriteAttributeHandler(this));
            } else {
                registry.registerReadOnlyAttribute(attr, new TranslatedReadAttributeHandler(this, this));
            }
        }
    }
}
