package org.jboss.as.mail.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:04
 */
public class MailSubsystemResource extends SimpleResourceDefinition {


    public static final MailSubsystemResource INSTANCE = new MailSubsystemResource();

    private MailSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, MailExtension.SUBSYSTEM_NAME),
                MailExtension.getResourceDescriptionResolver(MailExtension.SUBSYSTEM_NAME));
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
        // Ops to add and remove the root resource
        final MailSubsystemAdd subsystemAdd = MailSubsystemAdd.INSTANCE;
        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, subsystemAdd, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, MailSubsystemRemove.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {

    }
}
