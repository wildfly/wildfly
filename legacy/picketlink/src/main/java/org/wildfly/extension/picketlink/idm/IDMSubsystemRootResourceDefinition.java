/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.wildfly.extension.picketlink.idm.model.PartitionManagerResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final IDMSubsystemRootResourceDefinition INSTANCE = new IDMSubsystemRootResourceDefinition();

    private IDMSubsystemRootResourceDefinition() {
        super(new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, IDMExtension.SUBSYSTEM_NAME),
                 IDMExtension.getResourceDescriptionResolver(IDMExtension.SUBSYSTEM_NAME))
                .setAddHandler(new ModelOnlyAddStepHandler())
                .setAddRestartLevel(OperationEntry.Flag.RESTART_NONE)
                .setRemoveHandler(ModelOnlyRemoveStepHandler.INSTANCE)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
        );
        setDeprecated(IDMExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(PartitionManagerResourceDefinition.INSTANCE);
    }
}
