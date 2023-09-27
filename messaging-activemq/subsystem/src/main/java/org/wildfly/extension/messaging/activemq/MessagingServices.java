/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.Capabilities.ACTIVEMQ_SERVER_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_BRIDGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SUBSYSTEM;

import java.util.function.Function;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class MessagingServices {

    public static final ServiceName ACTIVEMQ_CLIENT_THREAD_POOL = ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName().getParent().append("client-thread-pool");
    private static final ServiceName COMMAND_DISPATCHER_FACTORY = ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName().getParent().append("command-dispatcher-factory");

    // Cached by MessagingSubsystemAdd at the beginning of runtime processing
    static volatile CapabilityServiceSupport capabilityServiceSupport;

   public static ServiceName getActiveMQServiceName(PathAddress pathAddress) {
         // We need to figure out what ActiveMQ this operation is targeting.
        // We can get that from the "address" element of the operation, as the "server=x" part of
        // the address will specify the name of the ActiveMQ server

       // We are a handler for requests related to a jms-topic resource. Those reside on level below the server
        // resources in the resource tree. So we could look for the server in the 2nd to last element
        // in the PathAddress. But to be more generic and future-proof, we'll walk the tree looking
       PathAddress serverPathAddress = getActiveMQServerPathAddress(pathAddress);
       if (serverPathAddress != null && serverPathAddress.size() > 0) {
           return ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName(serverPathAddress.getLastElement().getValue());
       }
       return null;
   }

   public static PathAddress getActiveMQServerPathAddress(PathAddress pathAddress) {
       for (int i = pathAddress.size() - 1; i >=0; i--) {
           PathElement pe = pathAddress.getElement(i);
           if (CommonAttributes.SERVER.equals(pe.getKey())) {
               return pathAddress.subAddress(0, i + 1);
           }
       }
       return PathAddress.EMPTY_ADDRESS;
   }

    public static ServiceName getActiveMQServiceName() {
        return ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName().getParent();
    }

   public static ServiceName getActiveMQServiceName(String serverName) {
       if(serverName == null || serverName.isEmpty()) {
           return getActiveMQServiceName();
       }
      return ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName(serverName);
   }

   public static ServiceName getQueueBaseServiceName(ServiceName serverServiceName) {
       return serverServiceName.append(CommonAttributes.QUEUE);
   }

    static ServiceName getHttpUpgradeServiceName(String activemqServerName, String acceptorName) {
        return getActiveMQServiceName(activemqServerName).append("http-upgrade-service", acceptorName);
    }

    static ServiceName getLegacyHttpUpgradeServiceName(String activemqServerName, String acceptorName) {
        return getActiveMQServiceName(activemqServerName).append(CommonAttributes.LEGACY, "http-upgrade-service", acceptorName);
    }

    public static ServiceName getJMSBridgeServiceName(String bridgeName) {
       return ACTIVEMQ_SERVER_CAPABILITY.getCapabilityServiceName().getParent().append(JMS_BRIDGE).append(bridgeName);
    }

    public static ServiceName getBroadcastCommandDispatcherFactoryServiceName(String channelName) {
        return (channelName != null) ? COMMAND_DISPATCHER_FACTORY.append(channelName) : COMMAND_DISPATCHER_FACTORY;
    }

    /**
     * Determines a ServiceName from a capability name. Only supported for use by services installed by
     * this subsystem; will not function reliably until the subsystem has begun adding runtime services.
     *
     * @param capabilityBaseName the base name of the capability, or its full name if it is not dynamic
     * @param dynamicParts any dynamic parts of the capability name. May be {@code null}
     * @return the service name
     *
     * @throws IllegalStateException if invoked before the subsystem has begun adding runtime services
     */
    public static ServiceName getCapabilityServiceName(String capabilityBaseName, String... dynamicParts) {
        if (capabilityServiceSupport == null) {
            throw new IllegalStateException();
        }
        if (dynamicParts == null || dynamicParts.length == 0) {
            return capabilityServiceSupport.getCapabilityServiceName(capabilityBaseName);
        }
        return capabilityServiceSupport.getCapabilityServiceName(capabilityBaseName, dynamicParts);
    }

    /**
     * Name of the capability that ensures a local provider of transactions is present.
     * Once its service is started, calls to the getInstance() methods of ContextTransactionManager,
     * ContextTransactionSynchronizationRegistry and LocalUserTransaction can be made knowing
     * that the global default TM, TSR and UT will be from that provider.
     */
    public static final String LOCAL_TRANSACTION_PROVIDER_CAPABILITY = "org.wildfly.transactions.global-default-local-provider";

    /**
     * The capability name for the transaction XAResourceRecoveryRegistry.
     */
    public static final String TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY = "org.wildfly.transactions.xa-resource-recovery-registry";

    public static boolean isSubsystemResource(final OperationContext context) {
        return context.getCurrentAddress().size() > 0 && SUBSYSTEM.equals(context.getCurrentAddress().getParent().getLastElement().getKey());
    }

    public static class ServerNameMapper implements Function<PathAddress, String[]> {
        private final String name;
        public ServerNameMapper(String name) {
            this.name = name;
        }

        @Override
        public String[] apply(PathAddress pathAddress) {
            PathAddress serverAddress = getActiveMQServerPathAddress(pathAddress);
            if (serverAddress.size() > 0) {
                String servername = getActiveMQServerPathAddress(pathAddress).getLastElement().getValue();
                return new String[]{
                    servername,
                    name,
                    pathAddress.getLastElement().getValue()
                };
            }
            return new String[]{name,
                pathAddress.getLastElement().getValue()
            };
        }
    }
}
