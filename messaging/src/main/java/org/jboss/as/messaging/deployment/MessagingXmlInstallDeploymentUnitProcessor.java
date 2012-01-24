package org.jboss.as.messaging.deployment;

import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;

/**
 * Processor that handles the installation of the messaging subsystems depoyable XML
 *
 * @author Stuart Douglas
 */
public class MessagingXmlInstallDeploymentUnitProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ParseResult parseResult = deploymentUnit.getAttachment(MessagingAttachments.PARSE_RESULT);
        if (parseResult == null) {
            return;
        }

        for (final JmsDestination topic : parseResult.getTopics()) {
            final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(topic.getServer());
            String[] jndiBindings = null;
            if (topic.getDestination().hasDefined(ENTRIES.getName())) {
                final ModelNode entries = topic.getDestination().resolve().get(ENTRIES.getName());
                jndiBindings = JndiEntriesAttribute.getJndiBindings(entries);
            }
            JMSTopicAdd.INSTANCE.installServices(null, null, topic.getName(), hqServiceName, phaseContext.getServiceTarget(), jndiBindings);
        }

        for (final JmsDestination queue : parseResult.getQueues()) {

            final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(queue.getServer());
            String[] jndiBindings = null;
            final ModelNode destination = queue.getDestination();
            if (destination.hasDefined(ENTRIES.getName())) {
                final ModelNode entries = destination.resolve().get(ENTRIES.getName());
                jndiBindings = JndiEntriesAttribute.getJndiBindings(entries);
            }
            final String selector = destination.hasDefined(SELECTOR.getName()) ? destination.get(SELECTOR.getName()).resolve().asString() : null;
            final boolean durable = destination.hasDefined(DURABLE.getName()) ? destination.get(DURABLE.getName()).resolve().asBoolean() : false;

            JMSQueueAdd.INSTANCE.installServices(null, null, queue.getName(), phaseContext.getServiceTarget(), hqServiceName,selector, durable, jndiBindings);
        }

    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}
