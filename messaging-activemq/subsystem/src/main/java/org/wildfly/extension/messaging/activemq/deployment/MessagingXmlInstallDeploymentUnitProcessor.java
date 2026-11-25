/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_TOPIC;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;

import java.util.List;

import jakarta.jms.Queue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueConfigurationRuntimeHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueService;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicConfigurationRuntimeHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicService;

/**
 * Processor that handles the installation of the messaging subsystems deployable XML
 *
 * @author Stuart Douglas
 */
public class MessagingXmlInstallDeploymentUnitProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ParseResult> parseResults = deploymentUnit.getAttachmentList(MessagingAttachments.PARSE_RESULT);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        for (final ParseResult parseResult : parseResults) {

            for (final JmsDestination topic : parseResult.getTopics()) {
                final ServiceName serverServiceName = MessagingServices.getActiveMQServiceName(topic.getServer());
                String[] jndiBindings = null;
                if (topic.getDestination().hasDefined(CommonAttributes.DESTINATION_ENTRIES.getName())) {
                    final ModelNode entries = topic.getDestination().resolve().get(CommonAttributes.DESTINATION_ENTRIES.getName());
                    jndiBindings = JMSServices.getJndiBindings(entries);
                }
                JMSTopicService topicService = JMSTopicService.installService(topic.getName(), serverServiceName, phaseContext.getServiceTarget());
                final ServiceName topicServiceName = JMSServices.getJmsTopicBaseServiceName(serverServiceName).append(topic.getName());
                for (String binding : jndiBindings) {
                    BinderServiceUtil.installBinderService(phaseContext.getServiceTarget(), binding, topicService, topicServiceName);
                }

                //create the management registration
                final PathElement serverElement = PathElement.pathElement(SERVER, topic.getServer());
                final PathElement destination = PathElement.pathElement(JMS_TOPIC, topic.getName());
                deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
                PathAddress registration = PathAddress.pathAddress(serverElement, destination);
                createDeploymentSubModel(registration, deploymentUnit);

                JMSTopicConfigurationRuntimeHandler.INSTANCE.registerResource(topic.getServer(), topic.getName(), topic.getDestination());
            }

            for (final JmsDestination queue : parseResult.getQueues()) {

                final ServiceName serverServiceName = MessagingServices.getActiveMQServiceName(queue.getServer());
                String[] jndiBindings = null;
                final ModelNode destination = queue.getDestination();
                if (destination.hasDefined(CommonAttributes.DESTINATION_ENTRIES.getName())) {
                    final ModelNode entries = destination.resolve().get(CommonAttributes.DESTINATION_ENTRIES.getName());
                    jndiBindings = JMSServices.getJndiBindings(entries);
                }
                final String selector = destination.hasDefined(SELECTOR.getName()) ? destination.get(SELECTOR.getName()).resolve().asString() : null;
                final boolean durable = destination.hasDefined(DURABLE.getName()) ? destination.get(DURABLE.getName()).resolve().asBoolean() : false;

                Service<Queue> queueService = JMSQueueService.installService(queue.getName(), phaseContext.getServiceTarget(), serverServiceName, selector, durable);
                final ServiceName queueServiceName = JMSServices.getJmsQueueBaseServiceName(serverServiceName).append(queue.getName());
                for (String binding : jndiBindings) {
                    BinderServiceUtil.installBinderService(phaseContext.getServiceTarget(), binding, queueService, queueServiceName);

                }
                //create the management registration
                final PathElement serverElement = PathElement.pathElement(SERVER, queue.getServer());
                final PathElement dest = PathElement.pathElement(JMS_QUEUE, queue.getName());
                deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
                PathAddress registration = PathAddress.pathAddress(serverElement, dest);
                createDeploymentSubModel(registration, deploymentUnit);
                JMSQueueConfigurationRuntimeHandler.INSTANCE.registerResource(queue.getServer(), queue.getName(), destination);
            }
        }
    }


    @Override
    public void undeploy(final DeploymentUnit context) {
        final List<ParseResult> parseResults = context.getAttachmentList(MessagingAttachments.PARSE_RESULT);
        for (ParseResult parseResult : parseResults) {
            for (final JmsDestination topic : parseResult.getTopics()) {
                JMSTopicConfigurationRuntimeHandler.INSTANCE.unregisterResource(topic.getServer(), topic.getName());
            }

            for (final JmsDestination queue : parseResult.getQueues()) {
                JMSQueueConfigurationRuntimeHandler.INSTANCE.unregisterResource(queue.getServer(), queue.getName());
            }
        }
    }


    static void createDeploymentSubModel(final PathAddress address, final DeploymentUnit unit) {
        unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, address);
    }

}
