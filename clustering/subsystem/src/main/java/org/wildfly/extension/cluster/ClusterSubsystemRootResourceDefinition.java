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
    public static final ClusterSubsystemRootResourceDefinition INSTANCE = new ClusterSubsystemRootResourceDefinition();

    private ClusterSubsystemRootResourceDefinition() {
        super(ClusterExtension.SUBSYSTEM_PATH,
                ClusterExtension.getResourceDescriptionResolver(null),
                ClusterSubsystemAdd.INSTANCE,
                ClusterSubsystemRemove.INSTANCE);
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
        resourceRegistration.registerSubModel(new ManagementClientResourceDefinition());
    }
}
