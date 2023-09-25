/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import java.util.List;

import static org.jboss.as.jpa.subsystem.JPADefinition.DEFAULT_DATASOURCE;
import static org.jboss.as.jpa.subsystem.JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE;

public class JPADeploymentDefinition extends SimpleResourceDefinition {

    private static final PathElement ADDRESS = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME);

    JPADeploymentDefinition() {
        super(getParameters());
    }

    private static Parameters getParameters() {
        Parameters result = new Parameters(ADDRESS,
                JPAExtension.getResourceDescriptionResolver())
                .setFeature(false)
                .setRuntime();
        return result;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : List.of(DEFAULT_DATASOURCE, DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)) {
            resourceRegistration.registerReadOnlyAttribute(attribute, (context, operation) -> {
                PathAddress subsystemAddress = context.getCurrentAddress().getParent().getParent().append(ADDRESS);
                context.addStep(Util.getReadAttributeOperation(subsystemAddress, attribute.getName()), ReadAttributeHandler.RESOLVE_INSTANCE, context.getCurrentStage(), true);
            });
        }
    }
}
