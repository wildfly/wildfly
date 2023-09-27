/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Stefabo Maestri
 */
public class ConnectionPropertyDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_CONNECTION_PROPERTY = PathElement.pathElement(CONNECTION_PROPERTIES.getName());

    private final boolean deployed;

    ConnectionPropertyDefinition(final boolean deployed) {
        super(PATH_CONNECTION_PROPERTY,
                DataSourcesExtension.getResourceDescriptionResolver("data-source", "connection-properties"),
                deployed ? null : ConnectionPropertyAdd.INSTANCE,
                deployed ? null : ConnectionPropertyRemove.INSTANCE);
        this.deployed = deployed;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_PROPERTY_VALUE).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
            resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLDataSourceRuntimeHandler.INSTANCE);
        } else {
            resourceRegistration.registerReadOnlyAttribute(Constants.CONNECTION_PROPERTY_VALUE, null);
        }

    }

    static void registerTransformers11x(ResourceTransformationDescriptionBuilder parentBuilder) {
            parentBuilder.addChildResource(PATH_CONNECTION_PROPERTY)
             .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.CONNECTION_PROPERTY_VALUE)
                    .end();

        }

}
