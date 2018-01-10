/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.apache.activemq.artemis.api.core.JsonUtil.nullSafe;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.rollbackOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.resolveFilter;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.runtimeOnlyOperation;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.runtimeReadOnlyOperation;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicService.JMS_TOPIC_PREFIX;

import java.util.List;
import java.util.Map;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.api.core.Pair;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;
import org.apache.activemq.artemis.utils.JsonLoader;
import org.apache.activemq.artemis.utils.SelectorTranslator;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicReadAttributeHandler.DurabilityType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Handler for runtime operations that invoke on a ActiveMQ Topic.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicControlHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSTopicControlHandler INSTANCE = new JMSTopicControlHandler();

    private static final String REMOVE_MESSAGES = "remove-messages";
    private static final String DROP_ALL_SUBSCRIPTIONS = "drop-all-subscriptions";
    private static final String DROP_DURABLE_SUBSCRIPTION = "drop-durable-subscription";
    private static final String COUNT_MESSAGES_FOR_SUBSCRIPTION = "count-messages-for-subscription";
    private static final String LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON = "list-messages-for-subscription-as-json";
    private static final String LIST_MESSAGES_FOR_SUBSCRIPTION = "list-messages-for-subscription";
    private static final String LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-non-durable-subscriptions-as-json";
    private static final String LIST_NON_DURABLE_SUBSCRIPTIONS = "list-non-durable-subscriptions";
    private static final String LIST_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-durable-subscriptions-as-json";
    private static final String LIST_DURABLE_SUBSCRIPTIONS = "list-durable-subscriptions";
    private static final String LIST_ALL_SUBSCRIPTIONS_AS_JSON = "list-all-subscriptions-as-json";
    private static final String LIST_ALL_SUBSCRIPTIONS = "list-all-subscriptions";

    private static final AttributeDefinition CLIENT_ID = create(CommonAttributes.CLIENT_ID)
            .setRequired(true)
            .setValidator(new StringLengthValidator(1))
            .build();
    private static final AttributeDefinition SUBSCRIPTION_NAME = createNonEmptyStringAttribute("subscription-name");
    private static final AttributeDefinition QUEUE_NAME = createNonEmptyStringAttribute(CommonAttributes.QUEUE_NAME);

    private static final AttributeDefinition[] SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS = new AttributeDefinition[] {
            createNonEmptyStringAttribute("queueName"),
            createNonEmptyStringAttribute("clientID"),
            createNonEmptyStringAttribute("selector"),
            createNonEmptyStringAttribute("name"),
            create("durable", BOOLEAN).build(),
            create("messageCount", LONG).build(),
            create("deliveringCount", INT).build(),
            ObjectListAttributeDefinition.Builder.of("consumers",
                    ObjectTypeAttributeDefinition.Builder.of("consumers",
                            createNonEmptyStringAttribute("consumerID"),
                            createNonEmptyStringAttribute("connectionID"),
                            createNonEmptyStringAttribute("sessionID"),
                            create("browseOnly", BOOLEAN).build(),
                            create("creationTime", BOOLEAN).build()
                    ).build()
            ).build()
    };

    private JMSTopicControlHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        boolean readOnly = context.getResourceRegistration().getOperationFlags(PathAddress.EMPTY_ADDRESS, operationName).contains(OperationEntry.Flag.READ_ONLY);
        ServiceController<?> service = context.getServiceRegistry(!readOnly).getService(serviceName);
        ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
       ManagementService managementService = server.getManagementService();
       AddressControl control = AddressControl.class.cast(managementService.getResource(ResourceNames.ADDRESS + JMS_TOPIC_PREFIX + topicName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        try {
            if (LIST_ALL_SUBSCRIPTIONS.equals(operationName)) {
                String json = listAllSubscriptionsAsJSON(control, managementService);
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_ALL_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(listAllSubscriptionsAsJSON(control, managementService));
            } else if (LIST_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = listDurableSubscriptionsAsJSON(control, managementService);
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(listDurableSubscriptionsAsJSON(control, managementService));
            } else if (LIST_NON_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = listNonDurableSubscriptionsAsJSON(control, managementService);
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(listNonDurableSubscriptionsAsJSON(control, managementService));
            } else if (LIST_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                final String queueName = QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                String json = listMessagesForSubscriptionAsJSON(queueName, managementService);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON.equals(operationName)) {
                final String queueName = QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                context.getResult().set(listMessagesForSubscriptionAsJSON(queueName, managementService));
            } else if (COUNT_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                String clientId = CLIENT_ID.resolveModelAttribute(context, operation).asString();
                String subscriptionName = SUBSCRIPTION_NAME.resolveModelAttribute(context, operation).asString();
                String filter = resolveFilter(context, operation);
                context.getResult().set(countMessagesForSubscription(clientId, subscriptionName, filter, managementService));
            } else if (DROP_DURABLE_SUBSCRIPTION.equals(operationName)) {
                String clientId = CLIENT_ID.resolveModelAttribute(context, operation).asString();
                String subscriptionName = SUBSCRIPTION_NAME.resolveModelAttribute(context, operation).asString();
                dropDurableSubscription(clientId, subscriptionName, managementService);
                context.getResult();
            } else if (DROP_ALL_SUBSCRIPTIONS.equals(operationName)) {
                dropAllSubscriptions(control, managementService);
                context.getResult();
            } else if (REMOVE_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(removeMessages(filter, control, managementService));
            } else {
                // Bug
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.toString());
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_DURABLE_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_DURABLE_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_NON_DURABLE_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES_FOR_SUBSCRIPTION, resolver)
                .setParameters(QUEUE_NAME)
                .setReplyType(LIST)
                .setReplyParameters(JMSManagementHelper.JMS_MESSAGE_PARAMETERS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON, resolver)
                .setParameters(QUEUE_NAME)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(COUNT_MESSAGES_FOR_SUBSCRIPTION, resolver)
                .setParameters(CLIENT_ID, SUBSCRIPTION_NAME, FILTER)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(DROP_DURABLE_SUBSCRIPTION, resolver)
                .setParameters(CLIENT_ID, SUBSCRIPTION_NAME)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(DROP_ALL_SUBSCRIPTIONS, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(REMOVE_MESSAGES, resolver)
                .setParameters(FILTER)
                .setReplyType(INT)
                .build(),
                this);
    }

   private int removeMessages(final String filterStr, AddressControl addressControl, ManagementService managementService) throws Exception {
      String filter = createFilterFromJMSSelector(filterStr);
      int count = 0;
      String[] queues = addressControl.getQueueNames();
      for (String queue : queues) {
         QueueControl coreQueueControl = (QueueControl) managementService.getResource(ResourceNames.QUEUE + queue);
         if (coreQueueControl != null) {
            count += coreQueueControl.removeMessages(filter);
         }
      }

      return count;
   }


   private int countMessagesForSubscription(String clientID, String subscriptionName, String filterStr, ManagementService managementService) throws Exception {
      String queueName = ActiveMQDestination.createQueueNameForSubscription(true, clientID, subscriptionName);
      QueueControl coreQueueControl = (QueueControl) managementService.getResource(ResourceNames.QUEUE + queueName);
      if (coreQueueControl == null) {
         throw new IllegalArgumentException("No subscriptions with name " + queueName + " for clientID " + clientID);
      }
      String filter = createFilterFromJMSSelector(filterStr);
      return coreQueueControl.listMessages(filter).length;
   }

   private void dropAllSubscriptions(AddressControl addressControl, ManagementService managementService) throws Exception {
      ActiveMQServerControl serverControl = (ActiveMQServerControl) managementService.getResource(ResourceNames.BROKER);
      String[] queues = addressControl.getQueueNames();
      for (String queue : queues) {
         // Drop all subscription shouldn't delete the dummy queue used to identify if the topic exists on the core queues.
         // we will just ignore this queue
         if (!queue.equals(addressControl.getAddress())) {
            serverControl.destroyQueue(queue);
         }
      }
   }

   private void dropDurableSubscription(final String clientID, final String subscriptionName, ManagementService managementService) throws Exception {
      String queueName = ActiveMQDestination.createQueueNameForSubscription(true, clientID, subscriptionName);
      QueueControl coreQueueControl = (QueueControl) managementService.getResource(ResourceNames.QUEUE + queueName);
      if (coreQueueControl == null) {
         throw new IllegalArgumentException("No subscriptions with name " + queueName + " for clientID " + clientID);
      }
      ActiveMQServerControl serverControl = (ActiveMQServerControl) managementService.getResource(ResourceNames.BROKER);
      serverControl.destroyQueue(queueName, true);
   }

   private String listAllSubscriptionsAsJSON(AddressControl addressControl, ManagementService managementService)  {
      return listSubscribersInfosAsJSON(DurabilityType.ALL, addressControl, managementService);
   }

   private String listDurableSubscriptionsAsJSON(AddressControl addressControl, ManagementService managementService) throws Exception {
      return listSubscribersInfosAsJSON(DurabilityType.DURABLE, addressControl, managementService);
   }

   private String listNonDurableSubscriptionsAsJSON(AddressControl addressControl, ManagementService managementService) throws Exception {
      return listSubscribersInfosAsJSON(DurabilityType.NON_DURABLE, addressControl, managementService);
   }

   public String listMessagesForSubscriptionAsJSON(final String queueName, ManagementService managementService) throws Exception {
      return toJSON(listMessagesForSubscription(queueName, managementService));
   }

   private Map<String, Object>[] listMessagesForSubscription(final String queueName, ManagementService managementService) throws Exception {
      QueueControl coreQueueControl = (QueueControl) managementService.getResource(ResourceNames.QUEUE + queueName);
      if (coreQueueControl == null) {
         throw new IllegalArgumentException("No subscriptions with name " + queueName);
      }

      Map<String, Object>[] coreMessages = coreQueueControl.listMessages(null);

      Map<String, Object>[] jmsMessages = new Map[coreMessages.length];

      int i = 0;

      for (Map<String, Object> coreMessage : coreMessages) {
         jmsMessages[i++] = ActiveMQMessage.coreMaptoJMSMap(coreMessage);
      }
      return jmsMessages;
   }

   private String listSubscribersInfosAsJSON(final DurabilityType durability, AddressControl addressControl, ManagementService managementService)  {
      try {
         List<QueueControl> queues = JMSTopicReadAttributeHandler.getQueues(durability, addressControl, managementService);
         javax.json.JsonArrayBuilder array = JsonLoader.createArrayBuilder();

         for (QueueControl queue : queues) {
            String clientID = null;
            String subName = null;

            if (queue.isDurable() && queue.getRoutingType() == RoutingType.MULTICAST.toString()) {
               Pair<String, String> pair = ActiveMQDestination.decomposeQueueNameForDurableSubscription(queue.getName());
               clientID = pair.getA();
               subName = pair.getB();
            } else if (queue.getRoutingType() == RoutingType.MULTICAST.toString()) {
               // in the case of heirarchical topics the queue name will not follow the <part>.<part> pattern of normal
               // durable subscribers so skip decomposing the name for the client ID and subscription name and just
               // hard-code it
               clientID = "ActiveMQ";
               subName = "ActiveMQ";
            }

            String filter = queue.getFilter() != null ? queue.getFilter() : null;

            JsonObject info = JsonLoader.createObjectBuilder()
                    .add("queueName", queue.getName())
                    .add("clientID", nullSafe(clientID))
                    .add("selector", nullSafe(filter))
                    .add("name", nullSafe(subName))
                    .add("durable", queue.isDurable())
                    .add("messageCount", queue.getMessageCount())
                    .add("deliveringCount", queue.getDeliveringCount())
                    .add("consumers", queue.listConsumersAsJSON())
                    .build();

            array.add(info);
         }

         return array.build().toString();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   static String toJSON(final Map<String, Object>[] messages) {
      JsonArrayBuilder array = JsonLoader.createArrayBuilder();
      for (Map<String, Object> message : messages) {
         array.add(JsonUtil.toJsonObject(message));
      }
      return array.build().toString();
   }

   private static String createFilterFromJMSSelector(final String selectorStr) throws ActiveMQException {
      return selectorStr == null || selectorStr.trim().length() == 0 ? null : SelectorTranslator.convertToActiveMQFilterString(selectorStr);
   }

}
