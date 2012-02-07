package org.jboss.as.pojo;

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

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @created 7.2.12 14:41
 */
public class PojoResource extends SimpleResourceDefinition {
    public static final PojoResource INSTANCE = new PojoResource();

        private PojoResource() {
            super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, PojoExtension.SUBSYSTEM_NAME),
                    PojoExtension.getResourceDescriptionResolver(PojoExtension.SUBSYSTEM_NAME));
        }

        @Override
        public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
            final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
            // Ops to add and remove the root resource
            final PojoSubsystemAdd subsystemAdd = PojoSubsystemAdd.INSTANCE;
            final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
            rootResourceRegistration.registerOperationHandler(ADD, subsystemAdd, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
            rootResourceRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, new DefaultResourceRemoveDescriptionProvider(rootResolver), EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        }
}
