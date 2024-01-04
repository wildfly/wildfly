/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AdminObjectResourceDefinition extends SimpleResourceDefinition {

    private final boolean readOnly;

    public AdminObjectResourceDefinition(boolean readOnly) {
        super(PathElement.pathElement(ADMIN_OBJECTS_NAME), ResourceAdaptersExtension.getResourceDescriptionResolver(ADMIN_OBJECTS_NAME), readOnly ? null : AdminObjectAdd.INSTANCE, readOnly ? null : ReloadRequiredRemoveStepHandler.INSTANCE);
        this.readOnly = readOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        for (final AttributeDefinition attribute : CommonAttributes.ADMIN_OBJECTS_NODE_ATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new  ReloadRequiredWriteAttributeHandler(attribute));
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(AOConfigPropertyAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE));
    }
}
