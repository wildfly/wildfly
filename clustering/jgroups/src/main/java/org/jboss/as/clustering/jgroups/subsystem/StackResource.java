package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class StackResource extends SimpleResourceDefinition {

    static final PathElement STACK_PATH = PathElement.pathElement(ModelKeys.STACK);
    static final OperationStepHandler EXPORT_NATIVE_CONFIGURATION_HANDLER = new ExportNativeConfiguration();

    private final boolean runtimeRegistration;

    // attributes
    // operations
    static final OperationDefinition EXPORT_NATIVE_CONFIGURATION = new SimpleOperationDefinitionBuilder(ModelKeys.EXPORT_NATIVE_CONFIGURATION, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setReplyType(ModelType.STRING)
            .build();

    // registration
    public StackResource(boolean runtimeRegistration) {
        super(STACK_PATH,
                JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK),
                ProtocolStackAdd.INSTANCE,
                ProtocolStackRemove.INSTANCE);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        // register protocol add and remove
        resourceRegistration.registerOperationHandler(ProtocolResource.PROTOCOL_ADD, ProtocolResource.PROTOCOL_ADD_HANDLER);
        resourceRegistration.registerOperationHandler(ProtocolResource.PROTOCOL_REMOVE, ProtocolResource.PROTOCOL_REMOVE_HANDLER);
        // register export-native-configuration
        if (runtimeRegistration) {
            resourceRegistration.registerOperationHandler(StackResource.EXPORT_NATIVE_CONFIGURATION, StackResource.EXPORT_NATIVE_CONFIGURATION_HANDLER);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        // no stack attributes
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // child resources
        resourceRegistration.registerSubModel(TransportResource.INSTANCE);
        resourceRegistration.registerSubModel(ProtocolResource.INSTANCE);
    }
}
