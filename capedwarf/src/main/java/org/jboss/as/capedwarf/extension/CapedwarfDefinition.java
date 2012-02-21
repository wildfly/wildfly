package org.jboss.as.capedwarf.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * @author Tomaz Cerar
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class CapedwarfDefinition extends SimpleResourceDefinition {
    static final CapedwarfDefinition INSTANCE = new CapedwarfDefinition();

    protected static final SimpleAttributeDefinition APPENGINE_API =
            new SimpleAttributeDefinitionBuilder(CapedwarfModel.APPENGINE_API, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(CapedwarfModel.APPENGINE_API)
                    .setValidator(new StringLengthValidator(5, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    private CapedwarfDefinition() {
        super(PathElement.pathElement(
                ModelDescriptionConstants.SUBSYSTEM, CapedwarfExtension.SUBSYSTEM_NAME),
                CapedwarfExtension.getResourceDescriptionResolver(CapedwarfExtension.SUBSYSTEM_NAME)
        );
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();
        // Ops to add and remove the root resource
        final CapedwarfSubsystemAdd subsystemAdd = CapedwarfSubsystemAdd.INSTANCE;
        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, subsystemAdd, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(APPENGINE_API, null, new ReloadRequiredWriteAttributeHandler());
    }
}
