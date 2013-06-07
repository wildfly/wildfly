package org.wildfly.extension.cluster;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 */
public class ClusterSubsystemDefinition extends SimpleResourceDefinition {
    public static final ClusterSubsystemDefinition INSTANCE = new ClusterSubsystemDefinition();

    private ClusterSubsystemDefinition() {
        super(ClusterSubsystemExtension.SUBSYSTEM_PATH,
                ClusterSubsystemExtension.getResourceDescriptionResolver(null),
                //We always need to add an 'add' operation
                ClusterSubsystemAdd.INSTANCE,
                //Every resource that is added, normally needs a remove operation
                ClusterSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //you can register attributes here
    }
}
