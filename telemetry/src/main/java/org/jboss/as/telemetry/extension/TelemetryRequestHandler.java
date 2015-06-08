package org.jboss.as.telemetry.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

public class TelemetryRequestHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "enable";

    static final TelemetryRequestHandler INSTANCE = new TelemetryRequestHandler();

    static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, TelemetryExtension.getResourceDescriptionResolver(null))
            .setRuntimeOnly()
            .addParameter(TelemetrySubsystemDefinition.RHNUID)
            .addParameter(TelemetrySubsystemDefinition.RHNPW)
            .build();

    private TelemetryRequestHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String rhnUid = operation.require(TelemetryExtension.RHNUID).asString();
        final String rhnPw = operation.require(TelemetryExtension.RHNPW).asString();

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                TelemetryService service = (TelemetryService) context.getServiceRegistry(true).getRequiredService(TelemetryService.createServiceName()).getValue();
                service.setRhnLoginCredentials(rhnUid,rhnPw);
                service.setEnabled(true);
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
