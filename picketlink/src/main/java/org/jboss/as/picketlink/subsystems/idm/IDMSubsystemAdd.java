package org.jboss.as.picketlink.subsystems.idm;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.picketlink.PicketLinkLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMSubsystemAdd extends AbstractAddStepHandler {

    public static final IDMSubsystemAdd INSTANCE = new IDMSubsystemAdd();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        PicketLinkLogger.ROOT_LOGGER.activatingSubsystem();
    }
}
