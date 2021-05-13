package org.wildfly.extension.opentelemetry.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemRemove extends AbstractRemoveStepHandler {
    public static final OpenTelemetrySubsystemRemove INSTANCE = new OpenTelemetrySubsystemRemove();

    private OpenTelemetrySubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        //Remove any services installed by the corresponding add handler here
        //context.removeService(ServiceName.of("some", "name"));
    }


}
