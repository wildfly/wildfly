/**
 *
 */
package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.FILTER;

import java.util.List;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Core queue add update.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueAdd extends AbstractAddStepHandler {

    public static final QueueAdd INSTANCE = new QueueAdd();

    private QueueAdd() {}

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attributeDefinition : QueueDefinition.ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService != null) {
            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String queueName = address.getLastElement().getValue();
            final CoreQueueConfiguration queueConfiguration = createCoreQueueConfiguration(context, queueName, model);
            final QueueService service = new QueueService(queueConfiguration, false);
            final ServiceName queueServiceName = MessagingServices.getQueueBaseServiceName(hqServiceName).append(queueName);
            newControllers.add(context.getServiceTarget().addService(queueServiceName, service)
                    .addDependency(hqServiceName, HornetQServer.class, service.getHornetQService())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE)
                    .install());

        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addQueueConfigs
    }

    static void addQueueConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.QUEUE).asPropertyList()) {
                configs.add(createCoreQueueConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    private static CoreQueueConfiguration createCoreQueueConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final String queueAddress = QueueDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final ModelNode filterNode =  FILTER.resolveModelAttribute(context, model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        return new CoreQueueConfiguration(queueAddress, name, filter, durable);
    }

}
