package org.jboss.as.jacorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @created 6.1.12 23:00
 */
public class JacORBSubsystemResource extends SimpleResourceDefinition {
    public static final JacORBSubsystemResource INSTANCE = new JacORBSubsystemResource();
    private static final ReloadRequiredWriteAttributeHandler ATTRIBUTE_HANDLER = new ReloadRequiredWriteAttributeHandler();

    private JacORBSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME),
                JacORBExtension.getResourceDescriptionResolver(JacORBExtension.SUBSYSTEM_NAME));
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
        // Ops to add and remove the root resource
        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, JacORBSubsystemAdd.INSTANCE, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        for (AttributeDefinition attr : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr.getName(), null, ATTRIBUTE_HANDLER, flags);
        }

    }
}
