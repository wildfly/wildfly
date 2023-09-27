/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

/**
 * Definition for the datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class DataSourceDefinition extends AbstractDataSourceDefinition {

    static final PathElement PATH = pathElement("datasource");

    static final SimpleAttributeDefinition JTA_ATTRIBUTE = create("jta", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition CONNECTABLE_ATTRIBUTE = create("connectable", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList(asList(JTA_ATTRIBUTE, CONNECTABLE_ATTRIBUTE, JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE));

    // --- //

    DataSourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH, AgroalExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH))
                .setAddHandler(DataSourceOperations.ADD_OPERATION)
                .setRemoveHandler(DataSourceOperations.REMOVE_OPERATION)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(
                        new ApplicationTypeConfig(AgroalExtension.SUBSYSTEM_NAME, "datasource"))));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
