package org.jboss.as.logging;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoggingRootResource extends SimpleResourceDefinition {
    static final LoggingRootResource INSTANCE = new LoggingRootResource();

    private LoggingRootResource() {
        super(LoggingExtension.SUBSYSTEM_PATH,
                LoggingExtension.getResourceDescriptionResolver(),
                LoggingSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }
}
