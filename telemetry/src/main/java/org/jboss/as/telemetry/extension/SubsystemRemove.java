package org.jboss.as.telemetry.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author <a href="jkinlaw@redhat.com">Josh Kinlaw</a>
 */
class SubsystemRemove extends AbstractRemoveStepHandler {

    static final SubsystemRemove INSTANCE = new SubsystemRemove();

    private SubsystemRemove() {
    }
}
