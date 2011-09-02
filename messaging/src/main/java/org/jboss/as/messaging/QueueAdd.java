/**
 *
 */
package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;

import java.util.List;
import java.util.Locale;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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

    public static final QueueAdd INSTANCE = new QueueAdd();

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, subModel);
        return operation;
    }

    private QueueAdd() {}

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attributeDefinition : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
        if (hqService != null) {
            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String queueName = address.getLastElement().getValue();
            final CoreQueueConfiguration queueConfiguration = createCoreQueueConfiguration(queueName, model);
            final QueueService service = new QueueService(queueConfiguration, false);
            newControllers.add(context.getServiceTarget().addService(MessagingServices.CORE_QUEUE_BASE.append(queueName), service)
                    .addDependency(MessagingServices.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQService())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE)
                    .install());

        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addQueueConfigs
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getQueueAdd(locale);
    }

    static void addQueueConfigs(final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.QUEUE).asPropertyList()) {
                configs.add(createCoreQueueConfiguration(prop.getName(), prop.getValue()));

            }
        }
    }

    private static CoreQueueConfiguration createCoreQueueConfiguration(String name, ModelNode model) throws OperationFailedException {
        final String queueAddress = QUEUE_ADDRESS.validateResolvedOperation(model).asString();
        final ModelNode filterNode =  FILTER.validateResolvedOperation(model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final boolean durable = DURABLE.validateResolvedOperation(model).asBoolean();

        return new CoreQueueConfiguration(queueAddress, name, filter, durable);
    }

}
