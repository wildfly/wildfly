package org.wildfly.clustering.diagnostics.extension;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * The root resource of the cluster subsystem.
 *
 * /subsystem=cluster
 *
 * @author  Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteringDiagnosticsSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    private final boolean runtimeRegistration ;

    public ClusteringDiagnosticsSubsystemRootResourceDefinition(final boolean runtimeRegistration) {
        super(ClusteringDiagnosticsExtension.SUBSYSTEM_PATH,
                ClusteringDiagnosticsExtension.getResourceDescriptionResolver(null),
                ClusteringDiagnosticsSubsystemAdd.INSTANCE,
                ClusteringDiagnosticsSubsystemRemove.INSTANCE);
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
