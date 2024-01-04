/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsr77.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JSR77ManagementRootResource extends PersistentResourceDefinition {

    JSR77ManagementRootResource() {
        super(new SimpleResourceDefinition.Parameters(
                    PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JSR77ManagementExtension.SUBSYSTEM_NAME),
                    JSR77ManagementExtension.getResourceDescriptionResolver(JSR77ManagementExtension.SUBSYSTEM_NAME)
                )
                .setAddHandler(new ModelOnlyAddStepHandler())
                .setRemoveHandler(ModelOnlyRemoveStepHandler.INSTANCE)
                .setDeprecatedSince(JSR77ManagementExtension.CURRENT_MODEL_VERSION)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }


}
