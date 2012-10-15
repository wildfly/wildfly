package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.Channel;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

/**
 * Implements /subsystem=jgroups/stack=X/export-native-configuration() operation.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ExportNativeConfiguration extends AbstractRuntimeOnlyHandler {

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        PathAddress stackAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String stackName = stackAddress.getLastElement().getValue();

        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = ChannelFactoryService.getServiceName(stackName);
        try {
            ServiceController<?> controller = registry.getRequiredService(serviceName);
            controller.setMode(ServiceController.Mode.ACTIVE);
            try {
                ChannelFactory factory = ServiceContainerHelper.getValue(controller, ChannelFactory.class);
                // Create a temporary channel, but don't connect it
                Channel channel = factory.createChannel(UUID.randomUUID().toString());
                try {
                    // ProtocolStack.printProtocolSpecAsXML() is very hacky and only works on an uninitialized stack
                    List<Protocol> protocols = channel.getProtocolStack().getProtocols();
                    Collections.reverse(protocols);
                    ProtocolStack stack = new ProtocolStack();
                    stack.addProtocols(protocols);
                    context.getResult().set(stack.printProtocolSpecAsXML());
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                } finally {
                    channel.close();
                }
            } finally {
                controller.setMode(ServiceController.Mode.ON_DEMAND);
            }
        } catch (Exception e) {
            throw new OperationFailedException(e.getLocalizedMessage(), e);
        }
    }
}
