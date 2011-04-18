/**
 *
 */
package org.jboss.as.messaging;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Removes a queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class QueueRemove implements ModelRemoveOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static QueueRemove INSTANCE = new QueueRemove();

    private QueueRemove() {
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        ModelNode opAddr = operation.require(OP_ADDR);
        ModelNode compensatingOp = QueueAdd.getOperation(opAddr, context.getSubModel());
        PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> service = context.getServiceRegistry().getService(MessagingServices.CORE_QUEUE_BASE.append(name));
                    if (service != null) {
                        service.setMode(ServiceController.Mode.REMOVE);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOp);
    }

            @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueRemove(locale);
    }

}
