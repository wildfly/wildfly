/**
 *
 */
package org.jboss.as.messaging;

import java.util.Locale;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.dmr.ModelNode;

/**
 * Removes a queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueRemove extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static QueueRemove INSTANCE = new QueueRemove();

    private QueueRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        context.removeService(MessagingServices.CORE_QUEUE_BASE.append(name));
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueRemove(locale);
    }

}
