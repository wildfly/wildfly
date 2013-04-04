package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Channel;

/**
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class ChannelMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final ChannelMetricsHandler INSTANCE = new ChannelMetricsHandler();

    public enum ChannelMetrics {
        STATE(ChannelInstanceResource.STATE),
        VIEW(ChannelInstanceResource.VIEW);

        private static final Map<String, ChannelMetrics> MAP = new HashMap<String, ChannelMetrics>();

        static {
            for (ChannelMetrics metric : ChannelMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private ChannelMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static ChannelMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name and channel attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-1).getValue();
        String attrName = operation.require(NAME).asString();
        ChannelMetrics metric = ChannelMetrics.getStat(attrName);

        System.out.println("retrieving metric for attribute: " + attrName);

        // lookup the channel
        ServiceName channelServiceName = ChannelInstanceCustomResource.CHANNEL_PARENT.append(channelName);
        ServiceController<?> controller = context.getServiceRegistry(false).getService(channelServiceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getValue() != null;
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(String.format("Unknown metric %s", attrName));
        } else if (!started) {
            // when the cache service is not available, return a null result
        } else {
            Channel channel = (Channel) controller.getValue();
            switch (metric) {
                case VIEW:
                    result.set(channel.getView().toString());
                    break;
                case STATE:
                    // result.set(channel.getState());
                    break;
            }
            context.getResult().set(result);
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    /*
    public static void rollbackOperationWithChannelNotFound(OperationContext context, PathAddress address) {
        String message = new String("No channel exists at address " + address.toString());
        context.getFailureDescription().set(message);
        context.setRollbackOnly();
        context.stepCompleted();
    }
    */
}
