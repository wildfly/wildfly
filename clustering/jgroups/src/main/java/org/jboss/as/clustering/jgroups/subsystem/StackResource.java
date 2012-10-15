package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class StackResource extends SimpleResourceDefinition {

    static final PathElement STACK_PATH = PathElement.pathElement(ModelKeys.STACK);
    static final StackResource INSTANCE = new StackResource() ;
    static final OperationStepHandler EXPORT_NATIVE_CONFIGURATION = new ExportNativeConfiguration();

    // attributes

    // registration
    private StackResource() {
        super(STACK_PATH,
                JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK),
                ProtocolStackAdd.INSTANCE,
                ProtocolStackRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        // register protocol add and remove
        resourceRegistration.registerOperationHandler(ModelKeys.ADD_PROTOCOL,
                ProtocolResource.PROTOCOL_ADD,
                ProtocolResource.PROTOCOL_ADD_DESCRIPTOR);
        resourceRegistration.registerOperationHandler(ModelKeys.REMOVE_PROTOCOL,
                ProtocolResource.PROTOCOL_REMOVE,
                ProtocolResource.PROTOCOL_REMOVE_DESCRIPTOR);
        // register export-native-configuration
        resourceRegistration.registerOperationHandler(ModelKeys.EXPORT_NATIVE_CONFIGURATION,
                EXPORT_NATIVE_CONFIGURATION,
                ProtocolResource.EXPORT_NATIVE_CONFIGURATION_DESCRIPTOR);
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
