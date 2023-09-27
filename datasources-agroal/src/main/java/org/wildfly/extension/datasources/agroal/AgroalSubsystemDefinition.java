/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Definition for the Agroal subsystem
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class AgroalSubsystemDefinition extends PersistentResourceDefinition {
    static final PathElement PATH = pathElement(SUBSYSTEM, AgroalExtension.SUBSYSTEM_NAME);

    AgroalSubsystemDefinition() {
        super(PATH, AgroalExtension.SUBSYSTEM_RESOLVER, AgroalSubsystemOperations.ADD_OPERATION, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return emptyList();
    }

    @Override
    public List<PersistentResourceDefinition> getChildren() {
        return List.of(new DataSourceDefinition(), new XADataSourceDefinition(), new DriverDefinition());
    }
}
