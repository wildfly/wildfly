package org.jboss.as.jdkorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Tomaz Cerar
 * @created 6.1.12 23:00
 */
public class JDKORBSubsystemResource extends SimpleResourceDefinition {
    public static final JDKORBSubsystemResource INSTANCE = new JDKORBSubsystemResource();
    private static final ReloadRequiredWriteAttributeHandler ATTRIBUTE_HANDLER = new ReloadRequiredWriteAttributeHandler();

    private JDKORBSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JDKORBExtension.SUBSYSTEM_NAME),
                JDKORBExtension.getResourceDescriptionResolver(JDKORBExtension.SUBSYSTEM_NAME),
                JDKORBSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }


    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : JDKORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, ATTRIBUTE_HANDLER);
        }

    }
}
