package com.redhat.gss.extension;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;

public class RedhatAccessPluginSubsystemDefinition extends
        SimpleResourceDefinition {
    static final RedhatAccessPluginSubsystemDefinition INSTANCE = new RedhatAccessPluginSubsystemDefinition();

    private RedhatAccessPluginSubsystemDefinition() {
        super(RedhatAccessPluginExtension.SUBSYSTEM_PATH,
                RedhatAccessPluginExtension.getResourceDescriptionResolver(),
                RedhatAccessPluginSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

}
