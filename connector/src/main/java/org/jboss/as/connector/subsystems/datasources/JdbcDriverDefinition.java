/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS_INFO;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Stefano Maestri
 */
public class JdbcDriverDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DRIVER = PathElement.pathElement(JDBC_DRIVER_NAME);

    private final List<AccessConstraintDefinition> accessConstraints;

    JdbcDriverDefinition() {
        super(PATH_DRIVER,
                DataSourcesExtension.getResourceDescriptionResolver(JDBC_DRIVER_NAME),
                JdbcDriverAdd.INSTANCE,
                JdbcDriverRemove.INSTANCE);
        ApplicationTypeConfig atc = new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, JDBC_DRIVER_NAME);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : Constants.JDBC_DRIVER_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }
        resourceRegistration.registerReadOnlyAttribute(DRIVER_NAME, ReadResourceNameOperationStepHandler.INSTANCE);
        if (resourceRegistration.getProcessType().isServer()) {
            resourceRegistration.registerReadOnlyAttribute(DATASOURCE_CLASS_INFO, GetDataSourceClassInfoOperationHandler.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
