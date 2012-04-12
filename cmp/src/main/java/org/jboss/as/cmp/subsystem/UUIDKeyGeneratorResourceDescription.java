package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stuart Douglas
 */
public class UUIDKeyGeneratorResourceDescription extends SimpleResourceDefinition {

    public static final UUIDKeyGeneratorResourceDescription INSTANCE = new UUIDKeyGeneratorResourceDescription();

    private UUIDKeyGeneratorResourceDescription() {
        super(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH,
                CmpExtension.getResourceDescriptionResolver(CmpSubsystemModel.UUID_KEY_GENERATOR),
                UUIDKeyGeneratorAdd.INSTANCE, UUIDKeyGeneratorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

    }
}
