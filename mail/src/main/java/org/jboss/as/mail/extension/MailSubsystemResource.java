package org.jboss.as.mail.extension;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:04
 */
public class MailSubsystemResource extends SimpleResourceDefinition {
    public static final MailSubsystemResource INSTANCE = new MailSubsystemResource();

    private MailSubsystemResource() {
        super(MailExtension.SUBSYSTEM_PATH,
                MailExtension.getResourceDescriptionResolver(),
                MailSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }
}
