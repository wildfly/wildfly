package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Stuart Douglas
 */
public class CMPSubsystemRootResourceDescription extends SimpleResourceDefinition {

    public static final CMPSubsystemRootResourceDescription INSTANCE = new CMPSubsystemRootResourceDescription();

    private CMPSubsystemRootResourceDescription() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, CmpExtension.SUBSYSTEM_NAME),
                CmpExtension.getResourceDescriptionResolver(CmpExtension.SUBSYSTEM_NAME),
                CmpSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }
}
