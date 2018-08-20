/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.datasources.agroal.AgroalExtension.getResolver;

/**
 * Definition for the datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class DataSourceDefinition extends AbstractDataSourceDefinition {

    static final SimpleAttributeDefinition JTA_ATTRIBUTE = create("jta", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition CONNECTABLE_ATTRIBUTE = create("connectable", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList(asList(JTA_ATTRIBUTE, CONNECTABLE_ATTRIBUTE, JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE));

    static final DataSourceDefinition INSTANCE = new DataSourceDefinition();

    // --- //

    private DataSourceDefinition() {
        // TODO The cast to PersistentResourceDefinition.Parameters is a workaround to WFCORE-4040
        super((Parameters) new Parameters(pathElement("datasource"), getResolver("datasource"))
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
