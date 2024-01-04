/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stefano Maestri
 */
public class XaDataSourcePropertyDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_PROPERTIES = PathElement.pathElement(XADATASOURCE_PROPERTIES.getName());

    private final boolean deployed;

    XaDataSourcePropertyDefinition(final boolean deployed) {
        super(PATH_PROPERTIES,
                DataSourcesExtension.getResourceDescriptionResolver("xa-data-source", "xa-datasource-properties"),
                deployed ? null : XaDataSourcePropertyAdd.INSTANCE,
                deployed ? null : XaDataSourcePropertyRemove.INSTANCE);
        this.deployed = deployed;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(Constants.XADATASOURCE_PROPERTY_VALUE).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
            resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLXaDataSourceRuntimeHandler.INSTANCE);
        } else {
            resourceRegistration.registerReadOnlyAttribute(Constants.XADATASOURCE_PROPERTY_VALUE, null);
        }
    }

}
