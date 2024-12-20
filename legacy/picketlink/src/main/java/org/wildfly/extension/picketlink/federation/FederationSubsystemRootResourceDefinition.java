/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.wildfly.extension.picketlink.federation.model.FederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    FederationSubsystemRootResourceDefinition() {
        super(new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, FederationExtension.SUBSYSTEM_NAME),
                 FederationExtension.getResourceDescriptionResolver(FederationExtension.SUBSYSTEM_NAME))
                .setAddHandler(new ModelOnlyAddStepHandler())
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveHandler(ModelOnlyRemoveStepHandler.INSTANCE)
                .setDeprecatedSince(FederationExtension.DEPRECATED_SINCE)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FederationResourceDefinition());
        MigrateOperation.registerOperations(resourceRegistration, getResourceDescriptionResolver());
    }
}
