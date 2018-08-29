/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_BRIDGE;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class MessagingServices {

    /**
     * The service name is "jboss.messaging-activemq"
     */
    static final ServiceName JBOSS_MESSAGING_ACTIVEMQ = ServiceName.JBOSS.append(MessagingExtension.SUBSYSTEM_NAME);
    static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName ACTIVEMQ_CLIENT_THREAD_POOL = JBOSS_MESSAGING_ACTIVEMQ.append("client-thread-pool");

   public static ServiceName getActiveMQServiceName(PathAddress pathAddress) {
         // We need to figure out what ActiveMQ this operation is targeting.
        // We can get that from the "address" element of the operation, as the "server=x" part of
        // the address will specify the name of the ActiveMQ server

       // We are a handler for requests related to a jms-topic resource. Those reside on level below the server
        // resources in the resource tree. So we could look for the server in the 2nd to last element
        // in the PathAddress. But to be more generic and future-proof, we'll walk the tree looking
       PathAddress serverPathAddress = getActiveMQServerPathAddress(pathAddress);
       if (serverPathAddress != null && serverPathAddress.size() > 0) {
           return JBOSS_MESSAGING_ACTIVEMQ.append(serverPathAddress.getLastElement().getValue());
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

   public static ServiceName getActiveMQServiceName(String serverName) {
       if(serverName == null || serverName.isEmpty()) {
           return JBOSS_MESSAGING_ACTIVEMQ;
       }
      return JBOSS_MESSAGING_ACTIVEMQ.append(serverName);
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
       return JBOSS_MESSAGING_ACTIVEMQ.append(JMS_BRIDGE).append(bridgeName);
   }

}
