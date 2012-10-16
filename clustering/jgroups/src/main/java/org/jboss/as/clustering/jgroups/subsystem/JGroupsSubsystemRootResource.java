package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsSubsystemRootResource extends SimpleResourceDefinition {

    // attributes
    static SimpleAttributeDefinition DEFAULT_STACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_STACK, ModelType.STRING, false)
                    .setXmlName(Attribute.DEFAULT_STACK.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    // registration
    JGroupsSubsystemRootResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                JGroupsExtension.getResourceDescriptionResolver(),
                JGroupsSubsystemAdd.INSTANCE,
                JGroupsSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_STACK, null, SubsystemWriteAttributeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
    }

}
