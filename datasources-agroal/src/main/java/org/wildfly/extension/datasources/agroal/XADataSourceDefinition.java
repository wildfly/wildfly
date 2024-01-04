/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;

/**
 * Definition for the xa-datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class XADataSourceDefinition extends AbstractDataSourceDefinition {

    static final PathElement PATH = pathElement("xa-datasource");
    static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList(asList(JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE));

    // --- //

    XADataSourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH, AgroalExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH))
                .setAddHandler(XADataSourceOperations.ADD_OPERATION)
                .setRemoveHandler(XADataSourceOperations.REMOVE_OPERATION)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(
                        new ApplicationTypeConfig(AgroalExtension.SUBSYSTEM_NAME, "xa-datasource"))));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
