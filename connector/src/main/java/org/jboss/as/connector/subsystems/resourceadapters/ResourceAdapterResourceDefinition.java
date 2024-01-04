/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector._private.Capabilities.RESOURCE_ADAPTER_CAPABILITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdapterResourceDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOLVER = ResourceAdaptersExtension.getResourceDescriptionResolver(RESOURCEADAPTER_NAME);
    private static final OperationDefinition ACTIVATE_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.ACTIVATE, RESOLVER).build();

    // The ManagedConnectionPool implementation used by default by versions < 4.0.0 (WildFly 10)

    private final boolean readOnly;
    private final boolean runtimeOnlyRegistrationValid;
    private final List<AccessConstraintDefinition> accessConstraints;

    public ResourceAdapterResourceDefinition(boolean readOnly, boolean runtimeOnlyRegistrationValid) {
        super(getParameters(readOnly));
        this.readOnly = readOnly;
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(ResourceAdaptersExtension.SUBSYSTEM_NAME, RESOURCEADAPTER_NAME);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    private static Parameters getParameters(boolean readOnly) {
        Parameters parameters = new Parameters(PathElement.pathElement(RESOURCEADAPTER_NAME), RESOLVER);
        if (!readOnly) {
            parameters.setAddHandler(RaAdd.INSTANCE)
                    .setRemoveHandler(RaRemove.INSTANCE)
                    .setCapabilities(RESOURCE_ADAPTER_CAPABILITY);
        }

        return parameters;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ACTIVATE_DEFINITION, RaActivate.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new  ReloadRequiredWriteAttributeHandler(attribute));

            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(readOnly ? null : ConfigPropertyAdd.INSTANCE, readOnly ? null : ReloadRequiredRemoveStepHandler.INSTANCE));
        resourceRegistration.registerSubModel(new ConnectionDefinitionResourceDefinition(readOnly, runtimeOnlyRegistrationValid));
        resourceRegistration.registerSubModel(new AdminObjectResourceDefinition(readOnly));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
