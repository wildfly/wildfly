/**
 *
 */
package org.jboss.as.messaging;

import java.util.List;
import java.util.Locale;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Core queue add update.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(QUEUE_ADDRESS).set(existing.get(ADDRESS));
        if (existing.hasDefined(FILTER)) {
            op.get(FILTER).set(existing.get(FILTER));
        }
        if (existing.hasDefined(DURABLE)) {
            op.get(DURABLE).set(existing.get(DURABLE));
        }
        return op;
    }

    public static QueueAdd INSTANCE = new QueueAdd();

    private final ParametersValidator validator = new ParametersValidator();

    private QueueAdd() {
        validator.registerValidator(QUEUE_ADDRESS, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        validator.registerValidator(FILTER, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        validator.registerValidator(DURABLE, new ModelTypeValidator(ModelType.BOOLEAN, true));
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String queueAddress = operation.hasDefined(QUEUE_ADDRESS) ? operation.get(QUEUE_ADDRESS).asString() : null;
        final String filter = operation.hasDefined(FILTER) ? operation.get(FILTER).asString() : null;
        final Boolean durable = operation.hasDefined(DURABLE) ? operation.get(DURABLE).asBoolean() : null;

        model.get(NAME).set(name);
        if (queueAddress != null) {
            model.get(ADDRESS).set(queueAddress);
        }
        if (filter != null) {
            model.get(FILTER).set(filter);
        }
        if (durable != null) {
            model.get(DURABLE).set(durable);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String queueAddress = operation.hasDefined(QUEUE_ADDRESS) ? operation.get(QUEUE_ADDRESS).asString() : null;
        final String filter = operation.hasDefined(FILTER) ? operation.get(FILTER).asString() : null;
        final Boolean durable = operation.hasDefined(DURABLE) ? operation.get(DURABLE).asBoolean() : null;

        final QueueService service = new QueueService(queueAddress, name, filter, durable != null ? durable : true, false);
        newControllers.add(context.getServiceTarget().addService(MessagingServices.CORE_QUEUE_BASE.append(name), service)
                .addDependency(MessagingServices.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQService())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueAdd(locale);
    }

}
