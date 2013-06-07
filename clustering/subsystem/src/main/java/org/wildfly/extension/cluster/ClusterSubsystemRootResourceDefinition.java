package org.wildfly.extension.cluster;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * The root resource of the cluster subsystem.
 *
 * /subsystem=cluster
 *
 * @author  Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    private final boolean runtimeRegistration ;

    public ClusterSubsystemRootResourceDefinition(final boolean runtimeRegistration) {
        super(ClusterExtension.SUBSYSTEM_PATH,
                ClusterExtension.getResourceDescriptionResolver(null),
                ClusterSubsystemAdd.INSTANCE,
                ClusterSubsystemRemove.INSTANCE);
        this.runtimeRegistration = runtimeRegistration;
    }

    public boolean isRuntimeRegistration() {
        return runtimeRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ClusterInstanceResourceDefinition(isRuntimeRegistration()));
    }
}
