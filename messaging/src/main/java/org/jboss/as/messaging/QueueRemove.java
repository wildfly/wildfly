/**
 *
 */
package org.jboss.as.messaging;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.RuntimeTask;
import org.jboss.as.server.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Removes a queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class QueueRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static QueueRemove INSTANCE = new QueueRemove();

    private QueueRemove() {
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        ModelNode opAddr = operation.require(OP_ADDR);
        ModelNode compensatingOp = QueueAdd.getOperation(opAddr, context.getSubModel());
        PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        if (context instanceof RuntimeOperationContext) {
            RuntimeOperationContext.class.cast(context).executeRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context, ResultHandler resultHandler) throws OperationFailedException {
                    final ServiceController<?> service = context.getServiceRegistry().getService(MessagingServices.CORE_QUEUE_BASE.append(name));
                    if (service != null) {
                        service.addListener(new ResultHandler.ServiceRemoveListener(resultHandler));
                    } else {
                        resultHandler.handleResultComplete();
                    }
                }
            }, resultHandler);
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
