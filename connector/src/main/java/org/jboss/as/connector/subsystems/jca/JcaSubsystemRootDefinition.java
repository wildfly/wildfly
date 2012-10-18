package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaSubsystemRootDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, JcaExtension.SUBSYSTEM_NAME);
    private final boolean registerRuntimeOnly;


    private JcaSubsystemRootDefinition(final boolean registerRuntimeOnly) {
        super(PATH_SUBSYSTEM,
                JcaExtension.getResourceDescriptionResolver(),
                JcaSubsystemAdd.INSTANCE,
                JcaSubSystemRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static JcaSubsystemRootDefinition createInstance(final boolean registerRuntimeOnly) {
        return new JcaSubsystemRootDefinition(registerRuntimeOnly);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);


    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(JcaArchiveValidationDefinition.INSTANCE);

        resourceRegistration.registerSubModel(JcaBeanValidationDefinition.INSTANCE);

        resourceRegistration.registerSubModel(JcaCachedConnectionManagerDefinition.INSTANCE);

        resourceRegistration.registerSubModel(JcaWorkManagerDefinition.createInstance(registerRuntimeOnly));

        resourceRegistration.registerSubModel(JcaBootstrapContextDefinition.INSTANCE);

    }
}
