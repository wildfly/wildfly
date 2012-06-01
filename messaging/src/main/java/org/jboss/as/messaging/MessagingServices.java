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

package org.jboss.as.messaging;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class MessagingServices {

    /** The service name "jboss.messaging". */
    private static final ServiceName JBOSS_MESSAGING = ServiceName.JBOSS.append("messaging");

    /** The core queue name base. */
    private static final String CORE_QUEUE_BASE = "queue";
    private static final String STARTUP_POOL = "startup-pool";

    public static enum TransportConfigType {
        Remote, InVM, Generic
    }

   public static ServiceName getHornetQServiceName(PathAddress pathAddress) {
         // We need to figure out what HornetQServer this operation is targeting.
        // We can get that from the "address" element of the operation, as the "hornetq-server=x" part of
        // the address will specify the name of the HornetQServer

       // We are a handler for requests related to a jms-topic resource. Those reside on level below the hornetq-server
        // resources in the resource tree. So we could look for the hornetq-server in the 2nd to last element
        // in the PathAddress. But to be more generic and future-proof, we'll walk the tree looking
        String hornetQServerName = null;
        for (int i = pathAddress.size() - 1; i >=0; i--) {
            PathElement pe = pathAddress.getElement(i);
            if (CommonAttributes.HORNETQ_SERVER.equals(pe.getKey())) {
                hornetQServerName = pe.getValue();
                break;
            }
        }
      return JBOSS_MESSAGING.append(hornetQServerName);
   }

   public static ServiceName getHornetQServiceName(String serverName) {
      return JBOSS_MESSAGING.append(serverName);
   }

   public static ServiceName getQueueBaseServiceName(ServiceName hornetqServiceName) {
       return hornetqServiceName.append(CORE_QUEUE_BASE);
   }

}
