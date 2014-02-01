package com.redhat.gss.extension;

import org.jboss.as.controller.SimpleResourceDefinition;

public class RedhatAccessPluginEapSubsystemDefinition extends SimpleResourceDefinition {
    static final RedhatAccessPluginEapSubsystemDefinition INSTANCE = new RedhatAccessPluginEapSubsystemDefinition();

    private RedhatAccessPluginEapSubsystemDefinition() {
        super(RedhatAccessPluginEapExtension.SUBSYSTEM_PATH, RedhatAccessPluginEapExtension.getResourceDescriptionResolver(),
                RedhatAccessPluginEapSubsystemAdd.INSTANCE,
                RedhatAccessPluginEapSubsystemRemove.INSTANCE);
    }

}
