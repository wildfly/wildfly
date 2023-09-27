/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.SUBSYSTEM_NAME;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdaptersRootResourceDefinition extends SimpleResourceDefinition {

    private final boolean runtimeOnlyRegistrationValid;

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(Constants.REPORT_DIRECTORY, null, new ReportDirectoryWriteHandler(Constants.REPORT_DIRECTORY));
    }

    public ResourceAdaptersRootResourceDefinition(boolean runtimeOnlyRegistrationValid) {
        super(ResourceAdaptersExtension.SUBSYSTEM_PATH, ResourceAdaptersExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME), ResourceAdaptersSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ResourceAdapterResourceDefinition(false, runtimeOnlyRegistrationValid));
    }

    @Override
    public void registerAdditionalRuntimePackages(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("jakarta.jms.api"));
    }
}
