package org.jboss.as.insights.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsFrequencyHandler extends AbstractWriteAttributeHandler<Void> {

    public static final InsightsFrequencyHandler INSTANCE = new InsightsFrequencyHandler();

    private InsightsFrequencyHandler() {
        super(InsightsSubsystemDefinition.FREQUENCY);
    }

    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        if (attributeName.equals(InsightsExtension.FREQUENCY)) {
            InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
            service.setFrequency(resolvedValue.asLong());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }

        return false;
    }

    /**
     * Hook to allow subclasses to revert runtime changes made in
     * {@link #applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}.
     *
     * @param context        the context of the operation
     * @param operation      the operation
     * @param attributeName  the name of the attribute being modified
     * @param valueToRestore the previous value for the attribute, before this operation was executed
     * @param valueToRevert  the new value for the attribute that should be reverted
     * @param handback       an object, if any, passed in to the {@code handbackHolder} by the {@code applyUpdateToRuntime}
     *                       implementation
     */
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
          ModelNode valueToRestore, ModelNode valueToRevert, Void handback) {
        // no-op
    }
}