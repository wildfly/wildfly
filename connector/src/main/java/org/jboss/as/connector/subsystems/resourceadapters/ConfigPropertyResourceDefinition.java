/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigPropertyResourceDefinition extends SimpleResourceDefinition {
    public ConfigPropertyResourceDefinition(AbstractAddStepHandler addHandler, OperationStepHandler removeHandler) {
        super(PathElement.pathElement(CONFIG_PROPERTIES.getName()), ResourceAdaptersExtension.getResourceDescriptionResolver(CONFIG_PROPERTIES.getName()), addHandler, removeHandler);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(Constants.CONFIG_PROPERTY_VALUE, null, new ReloadRequiredWriteAttributeHandler(Constants.CONFIG_PROPERTY_VALUE));
    }


}
