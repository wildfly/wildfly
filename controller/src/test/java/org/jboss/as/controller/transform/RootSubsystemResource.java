package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:04
 */
public class RootSubsystemResource extends SimpleResourceDefinition {


    public static final RootSubsystemResource INSTANCE = new RootSubsystemResource();

    private RootSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "test-subsystem"),
                new TestResourceDescriptionResolver());
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
        // Ops to add and remove the root resource

        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.completeStep();
            }
        }, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }
}
