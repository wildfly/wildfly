/**
 *
 */
package org.jboss.as.messaging;

import java.util.List;
import java.util.Locale;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.config.DivertConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.msc.service.ServiceRegistry;

/**
 * Core queue add update.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;


    private final ParametersValidator validator = new ParametersValidator();
    private final Configuration configuration;

    public QueueAdd(final Configuration configuration) {
        this.configuration = configuration;
        validator.registerValidator(QUEUE_ADDRESS.getName(), QUEUE_ADDRESS.getValidator());
        validator.registerValidator(FILTER.getName(), new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        validator.registerValidator(DURABLE.getName(), DURABLE.getValidator());
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        model.get(NAME).set(name);
        for (final AttributeDefinition attributeDefinition : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String queueName = address.getLastElement().getValue();
        final String queueAddress = ADDRESS.validateResolvedOperation(model).asString();
        final ModelNode filterNode =  FILTER.validateResolvedOperation(model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final boolean durable = DURABLE.validateResolvedOperation(model).asBoolean();

        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
        if (hqService != null) {
            final QueueService service = new QueueService(queueAddress, queueName, filter, durable, false);
            newControllers.add(context.getServiceTarget().addService(MessagingServices.CORE_QUEUE_BASE.append(queueName), service)
                    .addDependency(MessagingServices.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQService())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE)
                    .install());

        } else {
            // The initial subsystem install is not complete; just add our bit to the overall configuration
            List<CoreQueueConfiguration> queueConfigs = configuration.getQueueConfigurations();
            CoreQueueConfiguration queueConfig = new CoreQueueConfiguration(queueAddress, queueName, filter, durable);
            queueConfigs.add(queueConfig);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueAdd(locale);
    }

}
